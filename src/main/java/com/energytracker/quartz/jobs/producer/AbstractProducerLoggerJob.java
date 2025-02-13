package com.energytracker.quartz.jobs.producer;

import com.energytracker.dto.WeatherResponse;
import com.energytracker.entity.BaseProducer;
import com.energytracker.entity.CommercialProducer;
import com.energytracker.entity.Producer;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.influx.measurements.ProductionMeasurement;
import com.energytracker.quartz.util.MeasurementLogger;
import com.energytracker.webclients.WeatherApiClient;
import org.jetbrains.annotations.Nullable;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author André Heinen
 */
@Component
public abstract class AbstractProducerLoggerJob<T extends BaseProducer> implements Job {

	private final InfluxDBService influxDBService;
	private final WeatherApiClient weatherApiClient;

	private double sunPowerModificator = 1.0;
	private double windPowerModificator = 1.0;

	@Autowired
	protected AbstractProducerLoggerJob(InfluxDBService influxDBService, WeatherApiClient weatherApiClient) {
		this.influxDBService = influxDBService;
		this.weatherApiClient = weatherApiClient;
	}

	protected abstract List<T> getActiveProducers();
	protected abstract T getProducerById(Long id);
	protected abstract String getMeasurementName();

	protected abstract void updateAll(List<T> producerList);
	protected abstract void removeAll(List<T> producerList);
	protected abstract int getIntervalInSeconds();

	private final MeasurementLogger measurementLogger = new MeasurementLogger();

	@Override
	@Async("taskExecutor")
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		List<T> activeProducers = getActiveProducers();
		List<ProductionMeasurement> measurementsBatch = Collections.synchronizedList(new ArrayList<>());
		List<T> updatedProducers = Collections.synchronizedList(new ArrayList<>());
		List<T> removedProducers = Collections.synchronizedList(new ArrayList<>());

		WeatherResponse response = weatherApiClient.getWeather();
		if (response != null) {
			sunPowerModificator = response.getSolarPower();
			windPowerModificator = response.getWindPower();
		}
		System.out.println(sunPowerModificator + " / " + windPowerModificator);

		processASync(activeProducers, measurementsBatch, updatedProducers, removedProducers);

		updateDatabase(updatedProducers, removedProducers, measurementsBatch);

		measurementLogger.printAllEntries();
	}

	private void processSynced(
			List<T> activeProducers,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers
	) {
		for (T producer : activeProducers) {
			Long id = getDeviceId(producer);
			if (id != null) {
				T latestProducer = getProducerById(id);
				if (latestProducer != null) {
					processProducer(latestProducer, measurementsBatch, updatedProducers, removedProducers).join();
				}
			}
		}
	}

	private void processASync(
			List<T> activeProducers,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers
	) {
		List<CompletableFuture<Void>> tasks = new ArrayList<>();

		for (T producer : activeProducers) {
			Long id = getDeviceId(producer);
			if (id != null) {
				T latestProducer = getProducerById(id);
				if (latestProducer != null) {
					tasks.add(processProducer(latestProducer, measurementsBatch, updatedProducers, removedProducers));
				}
			}
		}

		CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
	}

	private void updateDatabase(List<T> updatedProducers, List<T> removedProducers, List<ProductionMeasurement> measurementsBatch) {
		if (!updatedProducers.isEmpty()) {
			updateAll(updatedProducers);
		}

		if (!removedProducers.isEmpty()) {
			removeAll(removedProducers);
		}

		if (!measurementsBatch.isEmpty()) {
			influxDBService.saveProductionMeasurements(measurementsBatch, getMeasurementName());
		}
	}

	public CompletableFuture<Void> processProducer(
			T producer,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers
	) {
		try {
			// Initialisierung der Start- und Endzeiten der Messung
			Instant startTime = initializeStartTime(producer);
			Instant endTime = calculateEndTime(producer);

			// Prüfen, ob das Gerät gerade erst gestartet wurde
			boolean justStarted = producer.getLastUpdate() == null;
			// Prüfen, ob das Gerät schon beendet werden kann
			boolean canFinish = producer.getEndTime() != null;

			Instant prev = producer.getLastUpdate() == null ? producer.getStartTime() : producer.getLastUpdate();
			Instant intervalStart = processStartPeriod(producer, justStarted, startTime, measurementsBatch);

			// Falls das Gerät neu gestartet wird und direkt beendet werden kann
			if (justStarted && canFinish && !intervalStart.isBefore(endTime)) {
				processSingleInterval(producer, startTime, endTime, measurementsBatch);
				prev = endTime;
			}

			// Verbrauchsdaten für wiederholte Intervalle verarbeiten
			prev = processIntervals(producer, prev, intervalStart, endTime, measurementsBatch);

			// Letztes Intervall und Abschlussverarbeitung, falls nötig
			if (canFinish) {
				processEndPeriod(producer, prev, measurementsBatch, removedProducers);
			}

			// Speichern der letzten Aktualisierungszeit und Hinzufügen zur aktualisierten Liste
			producer.setLastUpdate(endTime);
			updatedProducers.add(producer);

			return CompletableFuture.completedFuture(null);
		} catch (Exception e) {
			// Fehlerbehandlung mit detaillierter Ausgabe
			System.err.println("Fehler in der processProducer-Methode für " + producer.getClass().getSimpleName() + ": " + e.getMessage());
			return CompletableFuture.failedFuture(e);
		}
	}

	private Instant initializeStartTime(T producer) {
		if (producer.getLastUpdate() == null) {
			return producer.getStartTime();
		}
		return producer.getLastUpdate().plusSeconds(getIntervalInSeconds());
	}

	private Instant calculateEndTime(T producer) {
		if (producer.getEndTime() == null) {
			Instant currentTime = Instant.now();
			return currentTime.minusSeconds(currentTime.getEpochSecond() % getIntervalInSeconds())
					.truncatedTo(ChronoUnit.SECONDS);
		}
		return producer.getEndTime();
	}

	private Instant processStartPeriod(
			T producer,
			boolean justStarted,
			Instant startTime,
			List<ProductionMeasurement> measurementsBatch
	) {
		if (justStarted) {
			// Verbrauch initialisieren, wenn das Gerät gerade eingeschaltet wurde
			ProductionMeasurement zeroMeasurement = createZeroMeasurementAtStart(producer);
			measurementsBatch.add(zeroMeasurement);
			measurementLogger.addEntry(zeroMeasurement.getTimestamp(), String.format("Gerät angeschaltet. %.1f kWh für den Start gesetzt.", zeroMeasurement.getProduction()));

			Instant alignedStart = startTime.minusSeconds(startTime.getEpochSecond() % getIntervalInSeconds())
					.truncatedTo(ChronoUnit.SECONDS)
					.plusSeconds(getIntervalInSeconds());
			return alignedStart;
		}
		return startTime;
	}

	private void processSingleInterval(
			T producer,
			Instant startTime,
			Instant endTime,
			List<ProductionMeasurement> measurementsBatch
	) {
		long elapsedTime = getElapsedTimeInMilliseconds(startTime, endTime);
		double production = getProduction(producer, elapsedTime);

		ProductionMeasurement measurement = createMeasurement(producer, endTime, production);
		measurementsBatch.add(measurement);
		measurementLogger.addEntry(measurement.getTimestamp(), String.format(
				"Gerät ausgeschaltet. Nur ein Intervall mit %d ms. Erzeugung: %.6f kWh.",
				elapsedTime, measurement.getProduction()));
	}

	private Instant processIntervals(
			T producer,
			Instant prev,
			Instant intervalStart,
			Instant endTime,
			List<ProductionMeasurement> measurementsBatch
	) {
		while (!intervalStart.isAfter(endTime)) {
			long elapsedTime = getElapsedTimeInMilliseconds(prev, intervalStart);
			double production = getProduction(producer, elapsedTime);

			// Erfassen des Verbrauchs für das Intervall
			ProductionMeasurement measurement = createMeasurement(producer, intervalStart, production);
			measurementsBatch.add(measurement);

			measurementLogger.addEntry(measurement.getTimestamp(), String.format(
					"Intervall von %d ms erfasst. Erzeugung: %.6f kWh.",
					elapsedTime, production));

			prev = intervalStart;
			intervalStart = intervalStart.plusSeconds(getIntervalInSeconds());
		}
		return prev;
	}

	private void processEndPeriod(
			T producer,
			Instant prev,
			List<ProductionMeasurement> measurementsBatch,
			List<T> removedProducers
	) {
		if (!producer.getEndTime().equals(prev)) {
			// Letztes Intervall vor dem Ausschalten
			long lastIntervalElapsed = getElapsedTimeInMilliseconds(prev, producer.getEndTime());
			double production = getProduction(producer, lastIntervalElapsed);

			ProductionMeasurement lastMeasurement = createMeasurement(producer, producer.getEndTime(), production);
			measurementsBatch.add(lastMeasurement);

			measurementLogger.addEntry(lastMeasurement.getTimestamp(), String.format(
					"Gerät wurde ausgeschaltet. Letztes Intervall von %d ms erfasst. Erzeugung: %.6f kWh.",
					lastIntervalElapsed, lastMeasurement.getProduction()));
		}

		// Verbrauch nach dem Ende initialisieren und Verbraucher entfernen
		ProductionMeasurement zeroMeasurement = createZeroMeasurementAtEnd(producer);
		measurementsBatch.add(zeroMeasurement);
		removedProducers.add(producer);

		measurementLogger.addEntry(zeroMeasurement.getTimestamp(), String.format(
				"Gerät ist ausgeschaltet. %.1f kWh für das Ende gesetzt.",
				zeroMeasurement.getProduction()));
	}

	private static <T extends BaseProducer> @Nullable Long getDeviceId(T producer) {
		Long id = null;
		if (producer instanceof Producer updatedProducer) {
			id = updatedProducer.getId();
		} else if (producer instanceof CommercialProducer updatedProducer) {
			id = updatedProducer.getId();
		}
		return id;
	}

	private double getProduction(T producer, long durationInMilliseconds) {
		if (producer.getPowerType().equals("Solar Power")) {
			return ((producer.getPowerProduction() / 1000.0) * (durationInMilliseconds / (1000.0 * 3600.0)) * sunPowerModificator);
		} else if (producer.getPowerType().equals("Wind Power")) {
			return ((producer.getPowerProduction() / 1000.0) * (durationInMilliseconds / (1000.0 * 3600.0)) * windPowerModificator);
		} else {
			return ((producer.getPowerProduction() / 1000.0) * (durationInMilliseconds / (1000.0 * 3600.0)));
		}
	}

	private long getElapsedTimeInMilliseconds(Instant start, Instant end) {
		return end.toEpochMilli() - start.toEpochMilli();
	}

	private ProductionMeasurement createZeroMeasurementAtStart(T producer) {
		ProductionMeasurement measurement = new ProductionMeasurement();
		Instant startTime = producer.getStartTime();
		startTime = startTime.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(startTime);
		measurement.setDeviceId(producer.getDeviceId());
		measurement.setOwnerId(producer.getOwnerId());
		measurement.setPowerType(producer.getPowerType());
		measurement.setRenewable(producer.isRenewable());
		measurement.setProduction(0.0);
		return measurement;
	}

	private ProductionMeasurement createZeroMeasurementAtEnd(T producer) {
		ProductionMeasurement measurement = new ProductionMeasurement();
		Instant endTime = producer.getEndTime().plusMillis(1);
		endTime = endTime.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(endTime);
		measurement.setDeviceId(producer.getDeviceId());
		measurement.setOwnerId(producer.getOwnerId());
		measurement.setPowerType(producer.getPowerType());
		measurement.setRenewable(producer.isRenewable());
		measurement.setProduction(0.0);
		return measurement;
	}

	private ProductionMeasurement createMeasurement(T producer, Instant time, double production) {
		ProductionMeasurement measurement = new ProductionMeasurement();
		Instant timestamp = time.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(timestamp);
		measurement.setDeviceId(producer.getDeviceId());
		measurement.setOwnerId(producer.getOwnerId());
		measurement.setPowerType(producer.getPowerType());
		measurement.setRenewable(producer.isRenewable());
		measurement.setProduction(production);
		return measurement;
	}
}
