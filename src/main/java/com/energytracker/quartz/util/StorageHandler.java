package com.energytracker.quartz.util;

import com.energytracker.entity.BaseStorage;
import com.energytracker.entity.CommercialStorage;
import com.energytracker.entity.Storage;
import com.energytracker.influx.measurements.NetMeasurement;
import com.energytracker.influx.measurements.StorageMeasurement;
import com.energytracker.influx.service.general.InfluxMeasurementService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.service.GeneralDeviceService;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * @author André Heinen
 */
@Service
public class StorageHandler {

	private static final Logger logger = LoggerFactory.getLogger(StorageHandler.class);

	private static final ReentrantLock lock = new ReentrantLock(true);

	private final InfluxMeasurementService influxMeasurementService;
	private final GeneralDeviceService<CommercialStorage> commercialStorageService;
	private final GeneralDeviceService<Storage> storageService;
	private final InfluxDBClient influxDBClient;

	@Autowired
	public StorageHandler(InfluxMeasurementService influxMeasurementService, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService, InfluxDBClient influxDBClient) {
		this.influxMeasurementService = influxMeasurementService;
		this.commercialStorageService = commercialStorageService;
		this.storageService = storageService;
		this.influxDBClient = influxDBClient;
	}

	public int updateStorages(Map<Long, Double> totalConsumptionOrProductionOfOwnerMap, boolean isConsumption) {
		lock.lock();
		int measurementsCount;
		try {
			if (isConsumption) {
				measurementsCount = updateStoragesConsumption(totalConsumptionOrProductionOfOwnerMap);
			} else {
				measurementsCount = updateStoragesProduction(totalConsumptionOrProductionOfOwnerMap);
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
		return measurementsCount;
	}

	private int updateStoragesConsumption(Map<Long, Double> totalConsumptionOfOwnerMap) {
		List<StorageMeasurement> commercialStorageMeasurements = Collections.synchronizedList(new ArrayList<>());
		List<StorageMeasurement> storageMeasurements = Collections.synchronizedList(new ArrayList<>());
		NetMeasurement measurement = null;

		List<CommercialStorage> commercialStorages = commercialStorageService.getAll();
		List<Storage> storages = storageService.getAll();

		// 1. Gehe den Verbrauch pro Nutzer durch
		for (Map.Entry<Long, Double> entry : totalConsumptionOfOwnerMap.entrySet()) {
			Long ownerId = entry.getKey();
			double netConsumption = entry.getValue();

//			System.out.println("Consumption: " + consumption + " for owner: " + ownerId);

			Instant time = Instant.now();

			// 2. Hole die aktiven Storages des Nutzers aus den beiden Listen und sortiere sie nach consumingPriority (absteigend)
			List<BaseStorage> ownerStorages = Stream.concat(commercialStorages.stream(), storages.stream())
					.filter(storage -> storage.getOwnerId().equals(ownerId))
					.sorted((a, b) -> Integer.compare(b.getConsumingPriority(), a.getConsumingPriority()))
					.toList();

			// 3. Ziehe Verbrauch von den Benutzer-Storages ab
			netConsumption = goThroughStoragesForConsumption(ownerStorages, netConsumption, storageMeasurements, commercialStorageMeasurements, time);

			// 4. Wenn Benutzer keine Storages hat oder die Kapazität nicht ausreicht
			if (netConsumption > 0) {

				// Nach Priorität sortieren (höchste zuerst)
				commercialStorages.sort((a, b) -> Integer.compare(b.getConsumingPriority(), a.getConsumingPriority()));

				// Ziehe Verbrauch von den kommerziellen Storages ab
				netConsumption = goThroughStoragesForConsumption(commercialStorages, netConsumption, storageMeasurements, commercialStorageMeasurements, time);
			}

			// Falls immer noch Rest übrig ist, loggen wir diesen, da er nicht abgedeckt werden konnte
			if (netConsumption > 0) {
				measurement = new NetMeasurement();
				measurement.setTimestamp(time);
				double netBalance = getCurrentBalanceFromNetMeasurement();
				netBalance -= netConsumption;
				double netProductionChange = netConsumption * (-1.0);
				measurement.setCurrentBalance(netBalance);
				measurement.setChange(netProductionChange);
			}
		}
		return updateDatabase(commercialStorageMeasurements, storageMeasurements, measurement);
	}

	private int updateStoragesProduction(Map<Long, Double> totalProductionOfOwnerMap) {
		List<StorageMeasurement> commercialStorageMeasurements = Collections.synchronizedList(new ArrayList<>());
		List<StorageMeasurement> storageMeasurements = Collections.synchronizedList(new ArrayList<>());
		NetMeasurement measurement = null;

		List<CommercialStorage> commercialStorages = commercialStorageService.getAll();
		List<Storage> storages = storageService.getAll();

		// 1. Gehe die Produktion pro Nutzer durch
		for (Map.Entry<Long, Double> entry : totalProductionOfOwnerMap.entrySet()) {
			Long ownerId = entry.getKey();
			double netProduction = entry.getValue();

//			System.out.println("Production: " + production + " for owner: " + ownerId);

			Instant time = Instant.now();

			// 2. Hole die aktiven Storages des Nutzers aus den beiden Listen und sortiere sie nach chargingPriority (absteigend)
			List<BaseStorage> ownerStorages = Stream.concat(commercialStorages.stream(), storages.stream())
					.filter(storage -> storage.getOwnerId().equals(ownerId))
					.sorted((a, b) -> Integer.compare(b.getChargingPriority(), a.getChargingPriority()))
					.toList();

			// 3. Produktion auf die Storages des Nutzers verteilen
			netProduction = goThroughStoragesForProduction(ownerStorages, netProduction, storageMeasurements, commercialStorageMeasurements, time);

			// 4. Überschüssige Produktion bei kommerziellen Storages speichern
			if (netProduction > 0) {

				// Nach Priorität sortieren (höchste zuerst)
				commercialStorages.sort((a, b) -> Integer.compare(b.getChargingPriority(), a.getChargingPriority()));

				// Ziehe Verbrauch von den kommerziellen Storages ab
				netProduction = goThroughStoragesForProduction(commercialStorages, netProduction, storageMeasurements, commercialStorageMeasurements, time);
			}

			// Falls immer noch Rest übrig ist, loggen wir diesen, da er nicht abgedeckt werden konnte
			if (netProduction > 0) {
				measurement = new NetMeasurement();
				measurement.setTimestamp(time);
				double netBalance = getCurrentBalanceFromNetMeasurement();
				netBalance += netProduction;
				measurement.setCurrentBalance(netBalance);
				measurement.setChange(netProduction);
			}
		}
		return updateDatabase(commercialStorageMeasurements, storageMeasurements, measurement);
	}

	private synchronized double getCurrentBalanceFromNetMeasurement() {
		String fluxQuery = String.format(
				"from(bucket: \"%s\") "
						+ "|> range(start: -30d) "
						+ "|> filter(fn: (r) => r._measurement == \"%s\") "
						+ "|> filter(fn: (r) => r._field == \"currentBalance\") "
						+ "|> last()",
				InfluxConstants.BUCKET_NET, InfluxConstants.MEASUREMENT_NAME_NET
		);

		try {
			List<FluxTable> results = influxDBClient.getQueryApi().query(fluxQuery);
			Double currentBalance = findFirstDoubleInFluxTables(results);
			return Objects.requireNonNullElse(currentBalance, 0.0);

		} catch (Exception e) {
			logger.error("Fehler bei der Influx Query der NetBalance, 0.0 zurückgegeben", e);
			return 0.0;
		}
	}

	private Double findFirstDoubleInFluxTables(List<FluxTable> results) {
		if (results == null || results.isEmpty()) {
			return null;
		}

		// Prüfen, ob die Query Ergebnisse Punkte enthalten
		for (FluxTable table : results) {
			for (FluxRecord record : table.getRecords()) {
				if (record.getValue() instanceof Double) {
					return (Double) record.getValue();
				}
			}
		}

		return null;
	}

	private synchronized double getCurrentChargeFromStorage(Long deviceId) {
		// Query für das Measurement "commercial_storages"
		String fluxQueryCommercial = String.format(
				"from(bucket: \"%s\") "
						+ "|> range(start: -30d) "  // Bis zu 30 Tage zurück
						+ "|> filter(fn: (r) => r._measurement == \"%s\") " // Measurement filtern
						+ "|> filter(fn: (r) => r.deviceId == \"%s\") " // deviceId filtern
						+ "|> filter(fn: (r) => r._field == \"currentCharge\") " // Feld `currentCharge`
						+ "|> last()",  // Nur den letzten Wert auswählen
				InfluxConstants.BUCKET_STORAGE, InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL, deviceId
		);

		// Query für das Measurement "storages"
		String fluxQueryStorages = String.format(
				"from(bucket: \"%s\") "
						+ "|> range(start: -30d) "
						+ "|> filter(fn: (r) => r._measurement == \"%s\") "
						+ "|> filter(fn: (r) => r.deviceId == \"%s\") "
						+ "|> filter(fn: (r) => r._field == \"currentCharge\") "
						+ "|> last()",
				InfluxConstants.BUCKET_STORAGE, InfluxConstants.MEASUREMENT_NAME_STORAGE, deviceId
		);

		try {
			List<FluxTable> commercialResults = influxDBClient.getQueryApi().query(fluxQueryCommercial);
			Double commercialValue = findFirstDoubleInFluxTables(commercialResults);
			if (commercialValue != null) {
				return commercialValue; // Erfolgreich gefunden
			}

			// Fallback: Abfrage in "storages"
			List<FluxTable> storageResults = influxDBClient.getQueryApi().query(fluxQueryStorages);
			Double storageValue = findFirstDoubleInFluxTables(storageResults);
			return Objects.requireNonNullElse(storageValue, 0.0); // Erfolgreich gefunden

		} catch (Exception e) {
			logger.error("Fehler bei der Influx Query des Storage {}, 0.0 zurückgegeben", deviceId, e);
			return 0.0; // Bei Fehlern einfach 0.0 zurückgeben und den Fehler loggen
		}
	}

	private <T extends BaseStorage> double goThroughStoragesForConsumption(List<T> storages, double netConsumption, List<StorageMeasurement> storageMeasurements, List<StorageMeasurement> commercialStorageMeasurements, Instant time) {
		for (BaseStorage storage : storages) {
			double storageCurrentCharge = getCurrentChargeFromStorage(storage.getDeviceId());
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
		return netConsumption;
	}

	private <T extends BaseStorage> double goThroughStoragesForProduction(List<T> storages, double netProduction, List<StorageMeasurement> storageMeasurements, List<StorageMeasurement> commercialStorageMeasurements, Instant time) {
		for (BaseStorage storage : storages) {
			double storageCurrentCharge = getCurrentChargeFromStorage(storage.getDeviceId());
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
		return netProduction;
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

	private int updateDatabase(List<StorageMeasurement> commercialStorageMeasurements, List<StorageMeasurement> storageMeasurements, NetMeasurement netMeasurement) {

		if (!commercialStorageMeasurements.isEmpty()) {
			influxMeasurementService.saveStorageMeasurements(commercialStorageMeasurements, InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL);
		}

		if (!storageMeasurements.isEmpty()) {
			influxMeasurementService.saveStorageMeasurements(storageMeasurements, InfluxConstants.MEASUREMENT_NAME_STORAGE);
		}

		if (netMeasurement != null) {
			influxMeasurementService.saveNetMeasurement(netMeasurement, InfluxConstants.MEASUREMENT_NAME_NET);
		}

		return commercialStorageMeasurements.size() + storageMeasurements.size() + (netMeasurement != null ? 1 : 0);
	}
}
