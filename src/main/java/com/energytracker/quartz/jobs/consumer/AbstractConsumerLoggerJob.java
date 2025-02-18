package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.*;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.influx.measurements.ConsumptionMeasurement;
import com.energytracker.influx.measurements.StorageMeasurement;
import com.energytracker.service.GeneralDeviceService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * @author André Heinen
 */
@Component
public abstract class AbstractConsumerLoggerJob<T extends BaseConsumer> implements Job {

	private static final Logger logger = LoggerFactory.getLogger(AbstractConsumerLoggerJob.class);

	private static final int BATCH_SIZE = 500;

	private final InfluxDBService influxDBService;
	private final GeneralDeviceService<CommercialStorage> commercialStorageService;
	private final GeneralDeviceService<Storage> storageService;

	protected abstract List<T> getActiveConsumers();
	protected abstract T getConsumerById(Long id);
	protected abstract String getMeasurementName();
	protected abstract void updateAll(List<T> consumerList);
	protected abstract void removeAll(List<T> consumerList);
	protected abstract int getIntervalInSeconds();

	@Autowired
	public AbstractConsumerLoggerJob(InfluxDBService influxDBService, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService) {
		this.influxDBService = influxDBService;
		this.commercialStorageService = commercialStorageService;
		this.storageService = storageService;
	}

	@Override
	@Async("taskExecutor")
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		List<T> activeConsumers = getActiveConsumers();
		List<ConsumptionMeasurement> measurementsBatch = Collections.synchronizedList(new ArrayList<>());
		List<T> updatedConsumers = Collections.synchronizedList(new ArrayList<>());
		List<T> removedConsumers = Collections.synchronizedList(new ArrayList<>());
		Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData = new ConcurrentHashMap<>();
		List<StorageMeasurement> commercialStorageMeasurements = Collections.synchronizedList(new ArrayList<>());
		List<StorageMeasurement> storageMeasurements = Collections.synchronizedList(new ArrayList<>());

		processSyncedInBatches(activeConsumers, measurementsBatch, updatedConsumers, removedConsumers, ownerConsumptionData);

		Map<Long, Double> totalConsumptionOfOwnerMap = getTotalConsumptionByOwnerAsMap(ownerConsumptionData);
		updateStorages(commercialStorageMeasurements, storageMeasurements, totalConsumptionOfOwnerMap);

		List<ConsumptionMeasurement> totalConsumptionOfOwnerByTimestamp = calculateTotalConsumptionByOwnerAndTimestamp(ownerConsumptionData);
		List<ConsumptionMeasurement> totalConsumptionByTimestamp = calculateTotalConsumptionByTimestamp(ownerConsumptionData);
		updateDatabase(updatedConsumers, removedConsumers, measurementsBatch, commercialStorageMeasurements, storageMeasurements, totalConsumptionOfOwnerByTimestamp, totalConsumptionByTimestamp);
	}

	private void processSynced(
			List<T> activeConsumers,
			List<ConsumptionMeasurement> measurementsBatch,
			List<T> updatedConsumers,
			List<T> removedConsumers,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
	) {
		for (T consumer : activeConsumers) {
			Long id = getDeviceId(consumer);
			if (id != null) {
				T latestConsumer = getConsumerById(id);
				if (latestConsumer != null) {
					processConsumer(latestConsumer, measurementsBatch, updatedConsumers, removedConsumers, ownerConsumptionData);
				}
			}
		}
	}

	private void processSyncedInBatches(
			List<T> activeConsumers,
			List<ConsumptionMeasurement> measurementsBatch,
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
						processConsumer(latestConsumer, measurementsBatch, updatedConsumers, removedConsumers, ownerConsumptionData);
					}
				}
			}
		}
	}

	private StorageMeasurement createStorageMeasurement(Instant time, Long deviceId, Long ownerId, double capacity, double currentCharge) {
		StorageMeasurement measurement = new StorageMeasurement();
		measurement.setTimestamp(time);
		measurement.setDeviceId(deviceId.toString());
		measurement.setOwnerId(ownerId.toString());
		measurement.setCapacity(capacity);
		measurement.setCurrentCharge(currentCharge);
		return measurement;
	}

	private synchronized void updateStorages(
			List<StorageMeasurement> commercialStorageMeasurements,
			List<StorageMeasurement> storageMeasurements,
			Map<Long, Double> totalConsumptionOfOwnerMap
	) {

		List<CommercialStorage> commercialStorages = commercialStorageService.getAll();
		List<Storage> storages = storageService.getAll();

		// 1. Gehe den Verbrauch pro Nutzer durch
		for (Map.Entry<Long, Double> entry : totalConsumptionOfOwnerMap.entrySet()) {
			Long ownerId = entry.getKey();
			double consumption = entry.getValue();
			double netConsumption = consumption;

			Instant time = Instant.now();

			// 2. Hole die aktiven Storages des Nutzers aus den beiden Listen und sortiere sie nach consumingPriority (absteigend)
			List<BaseStorage> ownerStorages = Stream.concat(commercialStorages.stream(), storages.stream())
					.filter(storage -> storage.getOwnerId().equals(ownerId))
					.sorted((a, b) -> Integer.compare(b.getConsumingPriority(), a.getConsumingPriority()))
					.toList();

			// 3. Ziehe Verbrauch von den Benutzer-Storages ab
			for (BaseStorage storage : ownerStorages) {
				double storageCurrentCharge = influxDBService.getCurrentChargeFromStorage(storage.getDeviceId());
				if (netConsumption > 0) {
					double newCharge = Math.max(0, storageCurrentCharge - netConsumption);
					double consumed = storageCurrentCharge - newCharge;
					netConsumption -= consumed;

					// Erstelle ein Measurement für das neue `currentCharge` und füge es der Liste hinzu
					if (storage instanceof Storage) {
						storageMeasurements.add(createStorageMeasurement(time, storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
					} else if (storage instanceof CommercialStorage) {
						commercialStorageMeasurements.add(createStorageMeasurement(time, storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
					}
				}
			}

			// 4. Wenn Benutzer keine Storages hat oder die Kapazität nicht ausreicht
			if (netConsumption > 0) {

				// Nach Priorität sortieren (höchste zuerst)
				commercialStorages.sort((a, b) -> Integer.compare(b.getConsumingPriority(), a.getConsumingPriority()));

				// Ziehe Verbrauch von den kommerziellen Storages ab
				for (BaseStorage storage : commercialStorages) {
					double storageCurrentCharge = influxDBService.getCurrentChargeFromStorage(storage.getDeviceId());
					if (netConsumption > 0) {
						double newCharge = Math.max(0, storageCurrentCharge - netConsumption);
						double consumed = storageCurrentCharge - newCharge;
						netConsumption -= consumed;

						// Erstelle ein Measurement für das neue `currentCharge` und füge es der Liste hinzu
						if (storage instanceof Storage) {
							storageMeasurements.add(createStorageMeasurement(time, storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
						} else if (storage instanceof CommercialStorage) {
							commercialStorageMeasurements.add(createStorageMeasurement(time, storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
						}
					}
				}
			}

			// Falls immer noch Rest übrig ist, loggen wir diesen, da er nicht abgedeckt werden konnte
			if (netConsumption > 0) {
				System.out.println("Nicht genug Energie gespeichert. Dem NETZ wird " + String.format("%.6f", netConsumption) + " kWh abgezogen.");
				// TODO Hier wird der Rest später dem NETZ zugewiesen beziehungsweise abgezogen
			}
		}
	}

	private void updateDatabase(
			List<T> updatedConsumers,
			List<T> removedConsumers,
			List<ConsumptionMeasurement> measurementsBatch,
			List<StorageMeasurement> commercialStorageMeasurements,
			List<StorageMeasurement> storageMeasurements,
			List<ConsumptionMeasurement> totalConsumptionOfOwnerByTimestamp,
			List<ConsumptionMeasurement> totalConsumptionByTimestamp
	) {
		if (!updatedConsumers.isEmpty()) {
			updateAll(updatedConsumers);
		}

		if (!measurementsBatch.isEmpty()) {
			influxDBService.saveConsumptionMeasurements(measurementsBatch, getMeasurementName());
		}

		if (!commercialStorageMeasurements.isEmpty()) {
			influxDBService.saveStorageMeasurements(commercialStorageMeasurements, "storages_commercial");
		}

		if (!storageMeasurements.isEmpty()) {
			influxDBService.saveStorageMeasurements(storageMeasurements, "storages");
		}

		if (!totalConsumptionOfOwnerByTimestamp.isEmpty()) {
			influxDBService.saveConsumptionMeasurements(totalConsumptionOfOwnerByTimestamp, "consumption_owner");
		}

		if (!totalConsumptionByTimestamp.isEmpty()) {
			influxDBService.saveConsumptionMeasurements(totalConsumptionByTimestamp, "consumption_total");
		}

		if (!removedConsumers.isEmpty()) {
			removeAll(removedConsumers);
		}
	}

	public void processConsumer(
			T consumer,
			List<ConsumptionMeasurement> measurementsBatch,
			List<T> updatedConsumers,
			List<T> removedConsumers,
			Map<Long, List<ConsumptionMeasurement>> ownerConsumptionData
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

				// Füge Werte mit gleichem Zeitstempel zusammen
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
