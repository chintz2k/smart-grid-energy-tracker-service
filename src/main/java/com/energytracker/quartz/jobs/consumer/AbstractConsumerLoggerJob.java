package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.*;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.influx.measurements.ConsumptionMeasurement;
import org.jetbrains.annotations.Nullable;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.annotation.Async;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author André Heinen
 */
public abstract class AbstractConsumerLoggerJob<T extends BaseConsumer> implements Job {

	private final InfluxDBService influxDBService;

	protected AbstractConsumerLoggerJob(InfluxDBService influxDBService) {
		this.influxDBService = influxDBService;
	}

	protected abstract List<T> getActiveConsumers();
	protected abstract T getConsumerById(Long id);
	protected abstract String getMeasurementName();

	protected abstract void updateAll(List<T> consumerList);
	protected abstract void removeAll(List<T> consumerList);
	protected abstract int getIntervalInSeconds();

	private final Map<Instant, String> news = new ConcurrentHashMap<>();

	@Override
	@Async("taskExecutor")
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		List<T> activeConsumers = getActiveConsumers();
		List<ConsumptionMeasurement> measurementsBatch = Collections.synchronizedList(new ArrayList<>());
		List<T> updatedConsumers = Collections.synchronizedList(new ArrayList<>());
		List<T> removedConsumers = Collections.synchronizedList(new ArrayList<>());

		processASync(activeConsumers, measurementsBatch, updatedConsumers, removedConsumers);

		updateDatabase(updatedConsumers, removedConsumers, measurementsBatch);

		printNews();
	}

	private void processSynced(List<T> activeConsumers, List<ConsumptionMeasurement> measurementsBatch, List<T> updatedConsumers, List<T> removedConsumers) {
		for (T consumer : activeConsumers) {
			Long id = getDeviceId(consumer);
			if (id != null) {
				T latestConsumer = getConsumerById(id);
				if (latestConsumer != null) {
					processConsumer(latestConsumer, measurementsBatch, updatedConsumers, removedConsumers).join();
				}
			}
		}
	}

	private void processASync(List<T> activeConsumers, List<ConsumptionMeasurement> measurementsBatch, List<T> updatedConsumers, List<T> removedConsumers) {
		List<CompletableFuture<Void>> tasks = new ArrayList<>();

		for (T consumer : activeConsumers) {
			Long id = getDeviceId(consumer);
			if (id != null) {
				T latestConsumer = getConsumerById(id);
				if (latestConsumer != null) {
					tasks.add(processConsumer(latestConsumer, measurementsBatch, updatedConsumers, removedConsumers));
				}
			}
		}

		CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
	}

	private void updateDatabase(List<T> updatedConsumers, List<T> removedConsumers, List<ConsumptionMeasurement> measurementsBatch) {
		if (!updatedConsumers.isEmpty()) {
			updateAll(updatedConsumers);
		}

		if (!removedConsumers.isEmpty()) {
			removeAll(removedConsumers);
		}

		if (!measurementsBatch.isEmpty()) {
			influxDBService.saveMeasurements(measurementsBatch, getMeasurementName());
		}
	}

	public CompletableFuture<Void> processConsumer(
			T consumer,
			List<ConsumptionMeasurement> measurementsBatch,
			List<T> updatedConsumers,
			List<T> removedConsumers
	) {
		try {
			// Initialisierung der Start- und Endzeiten der Messung
			Instant startTime = initializeStartTime(consumer);
			Instant endTime = calculateEndTime(consumer);

			// Prüfen, ob das Gerät gerade erst gestartet wurde
			boolean justStarted = consumer.getLastUpdate() == null;
			// Prüfen, ob das Gerät schon beendet werden kann
			boolean canFinish = consumer.getEndTime() != null;

			Instant prev = consumer.getLastUpdate() == null ? consumer.getStartTime() : consumer.getLastUpdate();
			Instant intervalStart = processStartPeriod(consumer, justStarted, startTime, measurementsBatch);

			// Falls das Gerät neu gestartet wird und direkt beendet werden kann
			if (justStarted && canFinish && !intervalStart.isBefore(endTime)) {
				processSingleInterval(consumer, startTime, endTime, measurementsBatch);
				prev = endTime;
			}

			// Verbrauchsdaten für wiederholte Intervalle verarbeiten
			prev = processIntervals(consumer, prev, intervalStart, endTime, measurementsBatch);

			// Letztes Intervall und Abschlussverarbeitung falls nötig
			if (canFinish) {
				processEndPeriod(consumer, prev, measurementsBatch, removedConsumers);
			}

			// Speichern der letzten Aktualisierungszeit und Hinzufügen zur aktualisierten Liste
			consumer.setLastUpdate(endTime);
			updatedConsumers.add(consumer);

			return CompletableFuture.completedFuture(null);
		} catch (Exception e) {
			// Fehlerbehandlung mit detaillierter Ausgabe
			System.err.println("Fehler in der processConsumer-Methode für " + consumer.getClass().getSimpleName() + ": " + e.getMessage());
			return CompletableFuture.failedFuture(e);
		}
	}

	private Instant initializeStartTime(T consumer) {
		if (consumer.getLastUpdate() == null) {
			return consumer.getStartTime();
		}
		return consumer.getLastUpdate().plusSeconds(getIntervalInSeconds());
	}

	private Instant calculateEndTime(T consumer) {
		if (consumer.getEndTime() == null) {
			Instant currentTime = Instant.now();
			return currentTime.minusSeconds(currentTime.getEpochSecond() % getIntervalInSeconds())
					.truncatedTo(ChronoUnit.SECONDS);
		}
		return consumer.getEndTime();
	}

	private Instant processStartPeriod(
			T consumer,
			boolean justStarted,
			Instant startTime,
			List<ConsumptionMeasurement> measurementsBatch
	) {
		if (justStarted) {
			// Verbrauch initialisieren, wenn das Gerät gerade eingeschaltet wurde
			ConsumptionMeasurement zeroMeasurement = createZeroMeasurementAtStart(consumer);
			measurementsBatch.add(zeroMeasurement);
			addNews(zeroMeasurement.getTimestamp(), String.format("Gerät angeschaltet. %.1f kWh für den Start gesetzt.", zeroMeasurement.getConsumption()));

			Instant alignedStart = startTime.minusSeconds(startTime.getEpochSecond() % getIntervalInSeconds())
					.truncatedTo(ChronoUnit.SECONDS)
					.plusSeconds(getIntervalInSeconds());
			return alignedStart;
		}
		return startTime;
	}

	private void processSingleInterval(
			T consumer,
			Instant startTime,
			Instant endTime,
			List<ConsumptionMeasurement> measurementsBatch
	) {
		long elapsedTime = getElapsedTimeInMilliseconds(startTime, endTime);
		double consumption = getConsumption(consumer, elapsedTime);

		ConsumptionMeasurement measurement = createMeasurement(consumer, endTime, consumption);
		measurementsBatch.add(measurement);
		addNews(measurement.getTimestamp(), String.format(
				"Gerät ausgeschaltet. Nur ein Intervall mit %d ms. Verbrauch: %.6f kWh.",
				elapsedTime, measurement.getConsumption()));
	}

	private Instant processIntervals(
			T consumer,
			Instant prev,
			Instant intervalStart,
			Instant endTime,
			List<ConsumptionMeasurement> measurementsBatch
	) {
		while (!intervalStart.isAfter(endTime)) {
			long elapsedTime = getElapsedTimeInMilliseconds(prev, intervalStart);
			double consumption = getConsumption(consumer, elapsedTime);

			// Erfassen des Verbrauchs für das Intervall
			ConsumptionMeasurement measurement = createMeasurement(consumer, intervalStart, consumption);
			measurementsBatch.add(measurement);

			addNews(measurement.getTimestamp(), String.format(
					"Intervall von %d ms erfasst. Verbrauch: %.6f kWh.",
					elapsedTime, consumption));

			prev = intervalStart;
			intervalStart = intervalStart.plusSeconds(getIntervalInSeconds());
		}
		return prev;
	}

	private void processEndPeriod(
			T consumer,
			Instant prev,
			List<ConsumptionMeasurement> measurementsBatch,
			List<T> removedConsumers
	) {
		if (!consumer.getEndTime().equals(prev)) {
			// Letztes Intervall vor dem Ausschalten
			long lastIntervalElapsed = getElapsedTimeInMilliseconds(prev, consumer.getEndTime());
			double consumption = getConsumption(consumer, lastIntervalElapsed);

			ConsumptionMeasurement lastMeasurement = createMeasurement(consumer, consumer.getEndTime(), consumption);
			measurementsBatch.add(lastMeasurement);

			addNews(lastMeasurement.getTimestamp(), String.format(
					"Gerät wurde ausgeschaltet. Letztes Intervall von %d ms erfasst. Verbrauch: %.6f kWh.",
					lastIntervalElapsed, lastMeasurement.getConsumption()));
		}

		// Verbrauch nach dem Ende initialisieren und Verbraucher entfernen
		ConsumptionMeasurement zeroMeasurement = createZeroMeasurementAtEnd(consumer);
		measurementsBatch.add(zeroMeasurement);
		removedConsumers.add(consumer);

		addNews(zeroMeasurement.getTimestamp(), String.format(
				"Gerät ist ausgeschaltet. %.1f kWh für das Ende gesetzt.",
				zeroMeasurement.getConsumption()));
	}

	private static <T extends BaseConsumer> @Nullable Long getDeviceId(T consumer) {
		Long id = null;
		if (consumer instanceof Consumer updatedConsumer) {
			id = updatedConsumer.getId();
		} else if (consumer instanceof CommercialConsumer updatedConsumer) {
			id = updatedConsumer.getId();
		} else if (consumer instanceof SmartConsumer updatedConsumer) {
			id = updatedConsumer.getId();
		} else if (consumer instanceof CommercialSmartConsumer updatedConsumer) {
			id = updatedConsumer.getId();
		}
		return id;
	}

	private double getConsumption(T consumer, long durationInMilliseconds) {
		return (consumer.getPowerConsumption() / 1000.0) * (durationInMilliseconds / (1000.0 * 3600.0));
	}

	private long getElapsedTimeInMilliseconds(Instant start, Instant end) {
		return end.toEpochMilli() - start.toEpochMilli();
	}

	private ConsumptionMeasurement createZeroMeasurementAtStart(T consumer) {
		ConsumptionMeasurement measurement = new ConsumptionMeasurement();
		Instant startTime = consumer.getStartTime();
		startTime = startTime.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(startTime);
		measurement.setDeviceId(consumer.getDeviceId());
		measurement.setOwnerId(consumer.getOwnerId());
		measurement.setConsumption(0.0);
		return measurement;
	}

	private ConsumptionMeasurement createZeroMeasurementAtEnd(T consumer) {
		ConsumptionMeasurement measurement = new ConsumptionMeasurement();
		Instant endTime = consumer.getEndTime().plusMillis(1);
		endTime = endTime.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(endTime);
		measurement.setDeviceId(consumer.getDeviceId());
		measurement.setOwnerId(consumer.getOwnerId());
		measurement.setConsumption(0.0);
		return measurement;
	}

	private ConsumptionMeasurement createMeasurement(T consumer, Instant time, double consumption) {
		ConsumptionMeasurement measurement = new ConsumptionMeasurement();
		Instant timestamp = time.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(timestamp);
		measurement.setDeviceId(consumer.getDeviceId());
		measurement.setOwnerId(consumer.getOwnerId());
		measurement.setConsumption(consumption);
		return measurement;
	}

	private void addNews(Instant timestamp, String message) {
		news.put(timestamp, message);
	}

	private String getNews() {
		if (!news.isEmpty()) {
			StringBuilder sortedNews = new StringBuilder();

			news.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(entry -> sortedNews.append(entry.getKey())
							.append(" - ")
							.append(entry.getValue())
							.append("\n")
					);

			if (!sortedNews.isEmpty() && sortedNews.charAt(sortedNews.length() - 1) == '\n') {
				sortedNews.setLength(sortedNews.length() - 1);
			}

			return sortedNews.toString();
		}
		return null;
	}

	private void printNews() {
		if (getNews() != null) {
			System.out.println(getNews());
		}
	}
}
