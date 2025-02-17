package com.energytracker.quartz.jobs.producer;

import com.energytracker.dto.WeatherResponse;
import com.energytracker.entity.*;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.influx.measurements.ProductionMeasurement;
import com.energytracker.influx.measurements.StorageMeasurement;
import com.energytracker.service.GeneralDeviceService;
import com.energytracker.webclients.WeatherApiClient;
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
public abstract class AbstractProducerLoggerJob<T extends BaseProducer> implements Job {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProducerLoggerJob.class);

	private static final int BATCH_SIZE = 500;

	private final InfluxDBService influxDBService;
	private final WeatherApiClient weatherApiClient;
	private final GeneralDeviceService<CommercialStorage> commercialStorageService;
	private final GeneralDeviceService<Storage> storageService;

	protected abstract List<T> getActiveProducers();
	protected abstract T getProducerById(Long id);
	protected abstract String getMeasurementName();
	protected abstract void updateAll(List<T> producerList);
	protected abstract void removeAll(List<T> producerList);
	protected abstract int getIntervalInSeconds();

	private double sunPowerModificator = 1.0;
	private double windPowerModificator = 1.0;

	@Autowired
	public AbstractProducerLoggerJob(InfluxDBService influxDBService, WeatherApiClient weatherApiClient, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService) {
		this.influxDBService = influxDBService;
		this.weatherApiClient = weatherApiClient;
		this.commercialStorageService = commercialStorageService;
		this.storageService = storageService;
	}

	@Override
	@Async("taskExecutor")
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		List<T> activeProducers = getActiveProducers();
		List<ProductionMeasurement> measurementsBatch = Collections.synchronizedList(new ArrayList<>());
		List<T> updatedProducers = Collections.synchronizedList(new ArrayList<>());
		List<T> removedProducers = Collections.synchronizedList(new ArrayList<>());
		Map<Long, Double> userProductionMap = new ConcurrentHashMap<>();
		List<StorageMeasurement> commercialStorageMeasurements = Collections.synchronizedList(new ArrayList<>());
		List<StorageMeasurement> storageMeasurements = Collections.synchronizedList(new ArrayList<>());

		WeatherResponse response = weatherApiClient.getWeather();
		if (response != null) {
			sunPowerModificator = response.getSolarPower();
			windPowerModificator = response.getWindPower();
		}

		processSyncedInBatches(activeProducers, measurementsBatch, updatedProducers, removedProducers, userProductionMap);

		updateStorages(userProductionMap, commercialStorageMeasurements, storageMeasurements);

		updateDatabase(updatedProducers, removedProducers, measurementsBatch, commercialStorageMeasurements, storageMeasurements);
	}

	private void processSynced(
			List<T> activeProducers,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers,
			Map<Long, Double> userProductionMap
	) {
		for (T producer : activeProducers) {
			Long id = getDeviceId(producer);
			if (id != null) {
				T latestProducer = getProducerById(id);
				if (latestProducer != null) {
					processProducer(latestProducer, measurementsBatch, updatedProducers, removedProducers, userProductionMap);
				}
			}
		}
	}

	private void processSyncedInBatches(
			List<T> activeProducers,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers,
			Map<Long, Double> userProductionMap
	) {
		int totalProducers = activeProducers.size();

		for (int i = 0; i < totalProducers; i += BATCH_SIZE) {
			List<T> batch = activeProducers.subList(i, Math.min(i + BATCH_SIZE, totalProducers));

			for (T producer : batch) {
				Long id = getDeviceId(producer);
				if (id != null) {
					T latestProducer = getProducerById(id);
					if (latestProducer != null) {
						processProducer(latestProducer, measurementsBatch, updatedProducers, removedProducers, userProductionMap);
					}
				}
			}
		}
	}

	private StorageMeasurement createStorageMeasurement(Long deviceId, Long ownerId, double capacity, double currentCharge) {
		StorageMeasurement measurement = new StorageMeasurement();
		measurement.setTimestamp(Instant.now());
		measurement.setDeviceId(deviceId.toString());
		measurement.setOwnerId(ownerId.toString());
		measurement.setCapacity(capacity);
		measurement.setCurrentCharge(currentCharge);
		return measurement;
	}

	private synchronized void updateStorages(Map<Long, Double> userProductionMap, List<StorageMeasurement> commercialStorageMeasurements, List<StorageMeasurement> storageMeasurements) {

		List<CommercialStorage> commercialStorages = commercialStorageService.getAll();
		List<Storage> storages = storageService.getAll();

		// 1. Gehe die Produktion pro Nutzer durch
		for (Map.Entry<Long, Double> entry : userProductionMap.entrySet()) {
			Long ownerId = entry.getKey();
			double production = entry.getValue();
			double netProduction = production;

			// 2. Hole die aktiven Storages des Nutzers aus den beiden Listen und sortiere sie nach chargingPriority (absteigend)
			List<BaseStorage> ownerStorages = Stream.concat(commercialStorages.stream(), storages.stream())
					.filter(storage -> storage.getOwnerId().equals(ownerId))
					.sorted((a, b) -> Integer.compare(b.getChargingPriority(), a.getChargingPriority()))
					.toList();

			// 3. Produktion auf die Storages des Nutzers verteilen
			for (BaseStorage storage : ownerStorages) {
				double storageCurrentCharge = influxDBService.getCurrentChargeFromStorage(storage.getDeviceId());
				double capacity = storage.getCapacity();
				if (netProduction > 0) {
					double availableSpace = capacity - storageCurrentCharge;
					double toStore = Math.min(netProduction, availableSpace);
					double newCharge = storageCurrentCharge + toStore;

					// Update der Produktion und Speicherladung
					netProduction -= toStore;

					// Erstelle ein Measurement für das neue `currentCharge` und füge es der Liste hinzu
					if (storage instanceof Storage) {
						storageMeasurements.add(createStorageMeasurement(storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
					} else if (storage instanceof CommercialStorage) {
						commercialStorageMeasurements.add(createStorageMeasurement(storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
					}
				}
			}

			// 4. Überschüssige Produktion bei kommerziellen Storages speichern
			if (netProduction > 0) {

				// Nach Priorität sortieren (höchste zuerst)
				commercialStorages.sort((a, b) -> Integer.compare(b.getChargingPriority(), a.getChargingPriority()));

				// Ziehe Verbrauch von den kommerziellen Storages ab
				for (BaseStorage storage : commercialStorages) {
					double storageCurrentCharge = influxDBService.getCurrentChargeFromStorage(storage.getDeviceId());
					double capacity = storage.getCapacity();
					if (netProduction > 0) {
						double availableSpace = capacity - storageCurrentCharge;
						double toStore = Math.min(netProduction, availableSpace);
						double newCharge = storageCurrentCharge + toStore;

						// Update der Produktion und Speicherladung
						netProduction -= toStore;

						// Erstelle ein Measurement für das neue `currentCharge` und füge es der Liste hinzu
						if (storage instanceof Storage) {
							storageMeasurements.add(createStorageMeasurement(storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
						} else if (storage instanceof CommercialStorage) {
							commercialStorageMeasurements.add(createStorageMeasurement(storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
						}
					}
				}
			}

			// Falls immer noch Rest übrig ist, loggen wir diesen, da er nicht abgedeckt werden konnte
			if (netProduction > 0) {
				System.out.println("Überschüssige Energieproduktion für Nutzer: " + ownerId + ". " + netProduction + " kWh müssen dem NETZ zugefügt werden.");
				// TODO Hier wird der Rest später dem NETZ zugewiesen beziehungsweise abgezogen
			}
		}
	}

	private void updateDatabase(
			List<T> updatedProducers,
			List<T> removedProducers,
			List<ProductionMeasurement> measurementsBatch,
			List<StorageMeasurement> commercialStorageMeasurements,
			List<StorageMeasurement> storageMeasurements
	) {
		if (!updatedProducers.isEmpty()) {
			updateAll(updatedProducers);
		}

		if (!removedProducers.isEmpty()) {
			removeAll(removedProducers);
		}

		if (!measurementsBatch.isEmpty()) {
			influxDBService.saveProductionMeasurements(measurementsBatch, getMeasurementName());
		}

		if (!commercialStorageMeasurements.isEmpty()) {
			influxDBService.saveStorageMeasurements(commercialStorageMeasurements, "commercial_storages");
		}

		if (!storageMeasurements.isEmpty()) {
			influxDBService.saveStorageMeasurements(storageMeasurements, "storages");
		}

	}

	private void updateUserProduction(Long userId, double production, Map<Long, Double> userProductionMap) {
		userProductionMap.merge(userId, production, Double::sum);
	}

	public void processProducer(
			T producer,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers,
			Map<Long, Double> userProductionMap
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
				processSingleInterval(producer, startTime, endTime, measurementsBatch, userProductionMap);
				prev = endTime;
			}

			// Verbrauchsdaten für wiederholte Intervalle verarbeiten
			prev = processIntervals(producer, prev, intervalStart, endTime, measurementsBatch, userProductionMap);

			// Letztes Intervall und Abschlussverarbeitung, falls nötig
			if (canFinish) {
				processEndPeriod(producer, prev, measurementsBatch, removedProducers, userProductionMap);
			}

			// Speichern der letzten Aktualisierungszeit und Hinzufügen zur aktualisierten Liste
			producer.setLastUpdate(endTime);
			updatedProducers.add(producer);
		} catch (Exception e) {
			// Fehlerbehandlung mit detaillierter Ausgabe
			logger.error("Fehler in der processProducer-Methode für {}: {}", producer.getClass().getSimpleName(), e.getMessage());
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
			List<ProductionMeasurement> measurementsBatch,
			Map<Long, Double> userProductionMap
	) {
		long elapsedTime = getElapsedTimeInMilliseconds(startTime, endTime);
		double production = getProduction(producer, elapsedTime);

		ProductionMeasurement measurement = createMeasurement(producer, endTime, production, userProductionMap);
		measurementsBatch.add(measurement);
	}

	private Instant processIntervals(
			T producer,
			Instant prev,
			Instant intervalStart,
			Instant endTime,
			List<ProductionMeasurement> measurementsBatch,
			Map<Long, Double> userProductionMap
	) {
		while (!intervalStart.isAfter(endTime)) {
			long elapsedTime = getElapsedTimeInMilliseconds(prev, intervalStart);
			double production = getProduction(producer, elapsedTime);

			// Erfassen des Verbrauchs für das Intervall
			ProductionMeasurement measurement = createMeasurement(producer, intervalStart, production, userProductionMap);
			measurementsBatch.add(measurement);

			prev = intervalStart;
			intervalStart = intervalStart.plusSeconds(getIntervalInSeconds());
		}
		return prev;
	}

	private void processEndPeriod(
			T producer,
			Instant prev,
			List<ProductionMeasurement> measurementsBatch,
			List<T> removedProducers,
			Map<Long, Double> userProductionMap
	) {
		if (!producer.getEndTime().equals(prev)) {
			// Letztes Intervall vor dem Ausschalten
			long lastIntervalElapsed = getElapsedTimeInMilliseconds(prev, producer.getEndTime());
			double production = getProduction(producer, lastIntervalElapsed);

			ProductionMeasurement lastMeasurement = createMeasurement(producer, producer.getEndTime(), production, userProductionMap);
			measurementsBatch.add(lastMeasurement);
		}

		// Verbrauch nach dem Ende initialisieren und Verbraucher entfernen
		ProductionMeasurement zeroMeasurement = createZeroMeasurementAtEnd(producer);
		measurementsBatch.add(zeroMeasurement);
		removedProducers.add(producer);
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

	private ProductionMeasurement createMeasurement(
			T producer,
			Instant time,
			double production,
			Map<Long, Double> userProductionMap
	) {
		ProductionMeasurement measurement = new ProductionMeasurement();
		Instant timestamp = time.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(timestamp);
		measurement.setDeviceId(producer.getDeviceId());
		measurement.setOwnerId(producer.getOwnerId());
		measurement.setPowerType(producer.getPowerType());
		measurement.setRenewable(producer.isRenewable());
		measurement.setProduction(production);
		updateUserProduction(producer.getOwnerId(), production, userProductionMap);
		return measurement;
	}
}
