package com.energytracker.quartz.util;

import com.energytracker.entity.BaseStorage;
import com.energytracker.entity.CommercialStorage;
import com.energytracker.entity.Storage;
import com.energytracker.influx.InfluxConstants;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.influx.measurements.NetMeasurement;
import com.energytracker.influx.measurements.StorageMeasurement;
import com.energytracker.service.GeneralDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * @author André Heinen
 */
@Service
public class StorageHandler {

	private static final Logger logger = LoggerFactory.getLogger(StorageHandler.class);

	private static final ReentrantLock lock = new ReentrantLock(true);

	private final InfluxDBService influxDBService;
	private final GeneralDeviceService<CommercialStorage> commercialStorageService;
	private final GeneralDeviceService<Storage> storageService;

	@Autowired
	public StorageHandler(InfluxDBService influxDBService, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService) {
		this.influxDBService = influxDBService;
		this.commercialStorageService = commercialStorageService;
		this.storageService = storageService;
	}

	public void updateStorages(Map<Long, Double> totalConsumptionOrProductionOfOwnerMap, boolean isConsumption) {
		lock.lock();
		try {
			if (isConsumption) {
				updateStoragesConsumption(totalConsumptionOrProductionOfOwnerMap);
			} else {
				updateStoragesProduction(totalConsumptionOrProductionOfOwnerMap);
			}
		} catch (Exception e) {
			if (isConsumption) {
				logger.error("Update Storages with Consumption failed: {}", e.getMessage());
			} else {
				logger.error("Update Storages with Production failed: {}", e.getMessage());
			}
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	private void updateStoragesConsumption(Map<Long, Double> totalConsumptionOfOwnerMap) {
		List<StorageMeasurement> commercialStorageMeasurements = Collections.synchronizedList(new ArrayList<>());
		List<StorageMeasurement> storageMeasurements = Collections.synchronizedList(new ArrayList<>());
		NetMeasurement measurement = null;

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
				measurement = new NetMeasurement();
				measurement.setTimestamp(time);
				double netBalance = influxDBService.getCurrentBalanceFromNetMeasurement();
				netBalance -= netConsumption;
				double netProductionChange = netConsumption * (-1.0);
				measurement.setCurrentBalance(netBalance);
				measurement.setChange(netProductionChange);
			}
		}
		updateDatabase(commercialStorageMeasurements, storageMeasurements, measurement);
	}

	private void updateStoragesProduction(Map<Long, Double> totalProductionOfOwnerMap) {
		List<StorageMeasurement> commercialStorageMeasurements = Collections.synchronizedList(new ArrayList<>());
		List<StorageMeasurement> storageMeasurements = Collections.synchronizedList(new ArrayList<>());
		NetMeasurement measurement = null;

		List<CommercialStorage> commercialStorages = commercialStorageService.getAll();
		List<Storage> storages = storageService.getAll();

		// 1. Gehe die Produktion pro Nutzer durch
		for (Map.Entry<Long, Double> entry : totalProductionOfOwnerMap.entrySet()) {
			Long ownerId = entry.getKey();
			double production = entry.getValue();
			double netProduction = production;

			Instant time = Instant.now();

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
						storageMeasurements.add(createStorageMeasurement(time, storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
					} else if (storage instanceof CommercialStorage) {
						commercialStorageMeasurements.add(createStorageMeasurement(time, storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
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
							storageMeasurements.add(createStorageMeasurement(time, storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
						} else if (storage instanceof CommercialStorage) {
							commercialStorageMeasurements.add(createStorageMeasurement(time, storage.getDeviceId(), storage.getOwnerId(), storage.getCapacity(), newCharge));
						}
					}
				}
			}

			// Falls immer noch Rest übrig ist, loggen wir diesen, da er nicht abgedeckt werden konnte
			if (netProduction > 0) {
				measurement = new NetMeasurement();
				measurement.setTimestamp(time);
				double netBalance = influxDBService.getCurrentBalanceFromNetMeasurement();
				netBalance += netProduction;
				measurement.setCurrentBalance(netBalance);
				measurement.setChange(netProduction);
			}
		}
		updateDatabase(commercialStorageMeasurements, storageMeasurements, measurement);
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

	private void updateDatabase(List<StorageMeasurement> commercialStorageMeasurements, List<StorageMeasurement> storageMeasurements, NetMeasurement netMeasurement) {

		if (!commercialStorageMeasurements.isEmpty()) {
			influxDBService.saveStorageMeasurements(commercialStorageMeasurements, InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL);
		}

		if (!storageMeasurements.isEmpty()) {
			influxDBService.saveStorageMeasurements(storageMeasurements, InfluxConstants.MEASUREMENT_NAME_STORAGE);
		}

		if (netMeasurement != null) {
			influxDBService.saveNetMeasurement(netMeasurement, InfluxConstants.MEASUREMENT_NAME_NET);
		}
	}
}
