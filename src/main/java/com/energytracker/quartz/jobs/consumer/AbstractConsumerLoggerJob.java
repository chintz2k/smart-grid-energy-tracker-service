package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.devices.CommercialConsumer;
import com.energytracker.entity.devices.Consumer;
import com.energytracker.entity.devices.bases.BaseConsumer;
import com.energytracker.entity.monitoring.ConsumerProducerLoggerMonitor;
import com.energytracker.influx.measurements.devices.ConsumptionMeasurement;
import com.energytracker.influx.service.general.InfluxService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.quartz.util.StorageHandler;
import com.energytracker.service.monitoring.ConsumerProducerLoggerMonitorService;
import com.energytracker.webclients.DeviceApiClient;
import org.jetbrains.annotations.Nullable;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author André Heinen
 */
@Component
public abstract class AbstractConsumerLoggerJob<T extends BaseConsumer> implements Job {

	private static final Logger logger = LoggerFactory.getLogger(AbstractConsumerLoggerJob.class);

	private static final int BATCH_SIZE = 500;

	private final InfluxService influxService;
	private final StorageHandler storageHandler;
	private final ConsumerProducerLoggerMonitorService consumerProducerLoggerMonitorService;
	private final DeviceApiClient deviceApiClient;

	protected abstract List<T> getActiveConsumers(Instant startTime);
	protected abstract T getConsumerById(Long id);
	protected abstract String getMeasurementName();
	protected abstract void updateAll(List<T> consumerList);
	protected abstract void removeAll(List<T> consumerList);
	protected abstract int getIntervalInSeconds();

	@Autowired
	public AbstractConsumerLoggerJob(InfluxService influxService, StorageHandler storageHandler, ConsumerProducerLoggerMonitorService consumerProducerLoggerMonitorService, DeviceApiClient deviceApiClient) {
		this.influxService = influxService;
		this.storageHandler = storageHandler;
		this.consumerProducerLoggerMonitorService = consumerProducerLoggerMonitorService;
		this.deviceApiClient = deviceApiClient;
	}

	@Override
	@Async("taskExecutor")
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		try {
			long startTime = System.currentTimeMillis();

			List<T> activeConsumers = getActiveConsumers(Instant.now());
			long readActiveDevicesTime = System.currentTimeMillis() - startTime;

			List<ConsumptionMeasurement> measurementsBatch = Collections.synchronizedList(new ArrayList<>());
			Set<T> startedConsumers = Collections.synchronizedSet(new HashSet<>());
			List<T> updatedConsumers = Collections.synchronizedList(new ArrayList<>());
			List<T> removedConsumers = Collections.synchronizedList(new ArrayList<>());
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData = new ConcurrentHashMap<>();

			long beforeProcessMethod = System.currentTimeMillis();
			processSyncedInBatches(activeConsumers, measurementsBatch, startedConsumers, updatedConsumers, removedConsumers, ownerConsumptionData);
			long processMethodTime = System.currentTimeMillis() - beforeProcessMethod;

			long beforeUpdateStorages = System.currentTimeMillis();
			Map<Long, Double> totalConsumptionOfOwnerMap = getTotalConsumptionByOwnerAsMap(ownerConsumptionData);
			int measurementsCount = storageHandler.updateStorages(totalConsumptionOfOwnerMap, true);
			long updateStoragesTime = System.currentTimeMillis() - beforeUpdateStorages;

			List<ConsumptionMeasurement> totalConsumptionOfOwnerByTimestamp = calculateTotalConsumptionByOwnerAndTimestamp(ownerConsumptionData);
			List<ConsumptionMeasurement> totalConsumptionByTimestamp = calculateTotalConsumptionByTimestamp(ownerConsumptionData);

			measurementsCount = measurementsCount + measurementsBatch.size() + totalConsumptionOfOwnerByTimestamp.size() + totalConsumptionByTimestamp.size();
			int updatedConsumersCount = updatedConsumers.size();
			int removedConsumersCount = removedConsumers.size();
			long cpuIntensiveTime = System.currentTimeMillis() - startTime;

			long beforeWebRequests = System.currentTimeMillis();
			Set<Long> startedDeviceIds = new HashSet<>();
			for (T consumer : startedConsumers) {
				startedDeviceIds.add(consumer.getDeviceId());
			}
			deviceApiClient.setActiveByListAndNoSendEvent(startedDeviceIds, true, "consumers");
			Set<Long> removedDeviceIds = new HashSet<>();
			for (T consumer : removedConsumers) {
				removedDeviceIds.add(consumer.getDeviceId());
			}
			deviceApiClient.setActiveByListAndNoSendEvent(removedDeviceIds, false, "consumers");
			int startedConsumersCount = startedConsumers.size();
			long webRequestsTime = System.currentTimeMillis() - beforeWebRequests;

			long beforeUpdateDatabaseTime = System.currentTimeMillis();
			updateDatabase(updatedConsumers, removedConsumers, measurementsBatch, totalConsumptionOfOwnerByTimestamp, totalConsumptionByTimestamp);
			long updateDatabaseTime = System.currentTimeMillis() - beforeUpdateDatabaseTime;

			long overallTime = System.currentTimeMillis() - startTime;

			if (measurementsCount > 0) {
				consumerProducerLoggerMonitorService.save(
						new ConsumerProducerLoggerMonitor(
								Instant.now(),
								getClass().getSimpleName(),
								readActiveDevicesTime,
								processMethodTime,
								cpuIntensiveTime,
								updateStoragesTime,
								updateDatabaseTime,
								webRequestsTime,
								overallTime,
								startedConsumersCount,
								updatedConsumersCount,
								removedConsumersCount,
								measurementsCount
						)
				);
			}
		} catch (Exception e) {
			logger.error("Fehler in der execute-Methode: {}", e.getMessage());
			throw new JobExecutionException(e);
		}
	}

	private void processSyncedInBatches(
			List<T> activeConsumers,
			List<ConsumptionMeasurement> measurementsBatch,
			Set<T> startedConsumers,
			List<T> updatedConsumers,
			List<T> removedConsumers,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {
		int totalConsumers = activeConsumers.size();

		for (int i = 0; i < totalConsumers; i += BATCH_SIZE) {
			List<T> batch = activeConsumers.subList(i, Math.min(i + BATCH_SIZE, totalConsumers));

			for (T consumer : batch) {
				Long id = getDeviceId(consumer);
				if (id != null) {
					T latestConsumer = getConsumerById(id);
					if (latestConsumer != null) {
						processConsumer(latestConsumer, measurementsBatch, startedConsumers, updatedConsumers, removedConsumers, ownerConsumptionData);
					}
				}
			}
		}
	}

	private void updateDatabase(
			List<T> updatedConsumers,
			List<T> removedConsumers,
			List<ConsumptionMeasurement> measurementsBatch,
			List<ConsumptionMeasurement> totalConsumptionOfOwnerByTimestamp,
			List<ConsumptionMeasurement> totalConsumptionByTimestamp
	) {
		if (!updatedConsumers.isEmpty()) {
			updateAll(updatedConsumers);
		}

		if (!measurementsBatch.isEmpty()) {
			influxService.saveConsumptionMeasurements(measurementsBatch, getMeasurementName());
		}

		if (!totalConsumptionOfOwnerByTimestamp.isEmpty()) {
			influxService.saveConsumptionMeasurements(totalConsumptionOfOwnerByTimestamp, InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_OWNER);
		}

		if (!totalConsumptionByTimestamp.isEmpty()) {
			influxService.saveConsumptionMeasurements(totalConsumptionByTimestamp, InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_TOTAL);
		}

		if (!removedConsumers.isEmpty()) {
			removeAll(removedConsumers);
		}
	}

	public void processConsumer(
			T consumer,
			List<ConsumptionMeasurement> measurementsBatch,
			Set<T> startedConsumers,
			List<T> updatedConsumers,
			List<T> removedConsumers,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {
		try {
			// Initialisierung der Start- und Endzeiten der Messung
			Instant startTime = consumer.getLastUpdate() == null ? consumer.getStartTime() : consumer.getLastUpdate().plusSeconds(getIntervalInSeconds());
			Instant currentTime = Instant.now();
			Instant endTime = currentTime.minusSeconds(currentTime.getEpochSecond() % getIntervalInSeconds()).truncatedTo(ChronoUnit.SECONDS);

			// Prüfen, ob das Gerät gerade erst gestartet wurde
			boolean justStarted = consumer.getLastUpdate() == null;
			// Prüfen, ob das Gerät schon beendet werden kann
			boolean canFinish = false;
			if (consumer.getEndTime() != null) {
				if (!consumer.getEndTime().isAfter(endTime)) {
					canFinish = true;
					endTime = consumer.getEndTime();
				}
			}

			Instant prev = consumer.getLastUpdate() == null ? consumer.getStartTime() : consumer.getLastUpdate();
			Instant intervalStart = processStartPeriod(consumer, justStarted, startTime, measurementsBatch, startedConsumers);

			// Falls das Gerät neu gestartet wird und direkt beendet werden kann
			if (justStarted && canFinish && !intervalStart.isBefore(endTime)) {
				processSingleInterval(consumer, startTime, endTime, measurementsBatch, ownerConsumptionData);
				prev = endTime;
			}

			// Verbrauchsdaten für wiederholte Intervalle verarbeiten
			prev = processIntervals(consumer, prev, intervalStart, endTime, measurementsBatch, ownerConsumptionData);

			// Letztes Intervall und Abschlussverarbeitung, falls nötig
			if (canFinish) {
				processEndPeriod(consumer, prev, measurementsBatch, removedConsumers, ownerConsumptionData);
			}

			// Speichern der letzten Aktualisierungszeit und Hinzufügen zur aktualisierten Liste
			consumer.setLastUpdate(endTime);
			updatedConsumers.add(consumer);

		} catch (Exception e) {
			// Fehlerbehandlung mit detaillierter Ausgabe
			logger.error("Fehler in der processConsumer-Methode für {}: {}", consumer.getClass().getSimpleName(), e.getMessage());
		}
	}

	private Instant processStartPeriod(
			T consumer,
			boolean justStarted,
			Instant startTime,
			List<ConsumptionMeasurement> measurementsBatch,
			Set<T> startedConsumers
	) {
		if (justStarted) {
			// Verbrauch initialisieren, wenn das Gerät gerade eingeschaltet wurde
			ConsumptionMeasurement zeroMeasurement = createZeroMeasurementAtStart(consumer);
			measurementsBatch.add(zeroMeasurement);

			startedConsumers.add(consumer);

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
			List<ConsumptionMeasurement> measurementsBatch,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {
		long elapsedTime = getElapsedTimeInMilliseconds(startTime, endTime);
		double consumption = getConsumption(consumer, elapsedTime);

		ConsumptionMeasurement measurement = createMeasurement(consumer, endTime, consumption, ownerConsumptionData);
		measurementsBatch.add(measurement);
	}

	private Instant processIntervals(
			T consumer,
			Instant prev,
			Instant intervalStart,
			Instant endTime,
			List<ConsumptionMeasurement> measurementsBatch,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {
		while (!intervalStart.isAfter(endTime)) {
			long elapsedTime = getElapsedTimeInMilliseconds(prev, intervalStart);
			double consumption = getConsumption(consumer, elapsedTime);

			// Erfassen des Verbrauchs für das Intervall
			ConsumptionMeasurement measurement = createMeasurement(consumer, intervalStart, consumption, ownerConsumptionData);
			measurementsBatch.add(measurement);

			prev = intervalStart;
			intervalStart = intervalStart.plusSeconds(getIntervalInSeconds());
		}
		return prev;
	}

	private void processEndPeriod(
			T consumer,
			Instant prev,
			List<ConsumptionMeasurement> measurementsBatch,
			List<T> removedConsumers,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {
		if (!consumer.getEndTime().equals(prev)) {
			// Letztes Intervall vor dem Ausschalten
			long lastIntervalElapsed = getElapsedTimeInMilliseconds(prev, consumer.getEndTime());
			double consumption = getConsumption(consumer, lastIntervalElapsed);

			ConsumptionMeasurement lastMeasurement = createMeasurement(consumer, consumer.getEndTime(), consumption, ownerConsumptionData);
			measurementsBatch.add(lastMeasurement);
		}

		// Verbrauch nach dem Ende initialisieren und Verbraucher entfernen
		ConsumptionMeasurement zeroMeasurement = createZeroMeasurementAtEnd(consumer);
		measurementsBatch.add(zeroMeasurement);
		removedConsumers.add(consumer);
	}

	private static <T extends BaseConsumer> @Nullable Long getDeviceId(T consumer) {
		Long id = null;
		if (consumer instanceof Consumer updatedConsumer) {
			id = updatedConsumer.getId();
		} else if (consumer instanceof CommercialConsumer updatedConsumer) {
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

	private ConsumptionMeasurement createMeasurement(
			T consumer,
			Instant time,
			double consumption,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {
		ConsumptionMeasurement measurement = new ConsumptionMeasurement();
		Instant timestamp = time.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(timestamp);
		measurement.setDeviceId(consumer.getDeviceId());
		measurement.setOwnerId(consumer.getOwnerId());
		measurement.setConsumption(consumption);
		updateOwnerConsumptionHistory(consumer.getOwnerId(), measurement, ownerConsumptionData);
		return measurement;
	}

	private void updateOwnerConsumptionHistory(
			Long ownerId,
			ConsumptionMeasurement measurement,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {
		ownerConsumptionData.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(measurement);
	}

	private Map<Long, Double> getTotalConsumptionByOwnerAsMap(Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData) {
		// Erstelle eine neue Map, in der die Ergebnisse gespeichert werden.
		Map<Long, Double> totalConsumptionByOwner = new ConcurrentHashMap<>();

		// Iteriere über die bestehende Map und berechne den Gesamtverbrauch pro Owner
		ownerConsumptionData.forEach((ownerId, measurements) -> {
			double totalConsumption = measurements.stream()
					.mapToDouble(ConsumptionMeasurement::getConsumption)
					.sum();
			totalConsumptionByOwner.put(ownerId, totalConsumption);
		});

		return totalConsumptionByOwner;
	}

	private List<ConsumptionMeasurement> calculateTotalConsumptionByOwnerAndTimestamp(
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {

		// Liste, um die neuen Messungen zu sammeln
		List<ConsumptionMeasurement> totalMeasurements = new ArrayList<>();

		// Iteriere über jeden Owner und deren Verbrauchsdaten
		ownerConsumptionData.forEach((ownerId, measurements) -> {
			// Zusammenfassen des Verbrauchs pro Zeitstempel
			Map<Instant, Double> consumptionByTimestamp = new ConcurrentHashMap<>();

			// Gehe durch die Messungen und summiere den Verbrauch pro Zeitstempel
			for (ConsumptionMeasurement measurement : measurements) {
				Instant timestamp = measurement.getTimestamp();
				double consumption = measurement.getConsumption();

				// Füge Verbrauch mit gleichem Zeitstempel zusammen, also Gesamtverbrauch aller Geräte zu diesem Zeitstempel
				consumptionByTimestamp.merge(timestamp, consumption, Double::sum);
			}

			// Erstelle für jeden Zeitstempel ein neues ConsumptionMeasurement mit dem Gesamtverbrauch
			consumptionByTimestamp.forEach((timestamp, totalConsumption) -> {
				ConsumptionMeasurement totalMeasurement = new ConsumptionMeasurement();
				totalMeasurement.setTimestamp(timestamp);
				totalMeasurement.setDeviceId(null); // Kein spezifisches Device, da die Summe aller Devices umfasst ist
				totalMeasurement.setOwnerId(ownerId);
				totalMeasurement.setConsumption(totalConsumption);

				totalMeasurements.add(totalMeasurement);
			});
		});

		return totalMeasurements;
	}

	private List<ConsumptionMeasurement> calculateTotalConsumptionByTimestamp(
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {

		// Liste, um die neuen Messungen zu sammeln
		List<ConsumptionMeasurement> totalMeasurements = new ArrayList<>();

		// Map zur Aggregation des Verbrauchs je Timestamp
		Map<Instant, Double> consumptionByTimestamp = new ConcurrentHashMap<>();

		// Iteriere durch alle Owners und deren Verbrauchsdaten
		ownerConsumptionData.forEach((ownerId, measurements) -> {
			// Gehe durch jede Verbrauchsmessung des aktuellen Owners
			for (ConsumptionMeasurement measurement : measurements) {
				Instant timestamp = measurement.getTimestamp();
				double consumption = measurement.getConsumption();

				// Summiere den Verbrauch für jeden Zeitstempel
				consumptionByTimestamp.merge(timestamp, consumption, Double::sum);
			}
		});

		// Erstelle für jeden Zeitstempel ein neues ConsumptionMeasurement und füge es der Liste hinzu
		consumptionByTimestamp.forEach((timestamp, totalConsumption) -> {
			ConsumptionMeasurement totalMeasurement = new ConsumptionMeasurement();
			totalMeasurement.setTimestamp(timestamp);
			totalMeasurement.setDeviceId(null); // Kein spezifisches Device, da die Summe aller Devices umfasst ist
			totalMeasurement.setOwnerId(null); // Kein spezifischer Owner, da die Summe aller Owner umfasst ist
			totalMeasurement.setConsumption(totalConsumption);

			totalMeasurements.add(totalMeasurement);
		});

		return totalMeasurements;
	}
}
