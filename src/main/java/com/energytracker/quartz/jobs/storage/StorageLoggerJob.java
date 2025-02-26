package com.energytracker.quartz.jobs.storage;

import com.energytracker.entity.CommercialStorage;
import com.energytracker.entity.Storage;
import com.energytracker.entity.StorageLoggerMonitor;
import com.energytracker.influx.measurements.StorageMeasurement;
import com.energytracker.influx.service.general.InfluxMeasurementService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.service.GeneralDeviceService;
import com.energytracker.service.StorageLoggerMonitorService;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * @author André Heinen
 */
@Component
public class StorageLoggerJob implements Job {

	private final InfluxMeasurementService influxMeasurementService;
	private final InfluxQueryHelper influxQueryHelper;
	private final GeneralDeviceService<CommercialStorage> commercialStorageService;
	private final GeneralDeviceService<Storage> storageService;
	private final StorageLoggerMonitorService storageLoggerMonitorService;

	@Autowired
	public StorageLoggerJob(InfluxMeasurementService influxMeasurementService, InfluxQueryHelper influxQueryHelper, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService, StorageLoggerMonitorService storageLoggerMonitorService) {
		this.influxMeasurementService = influxMeasurementService;
		this.influxQueryHelper = influxQueryHelper;
		this.commercialStorageService = commercialStorageService;
		this.storageService = storageService;
		this.storageLoggerMonitorService = storageLoggerMonitorService;
	}

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		StorageLoggerMonitor monitor = new StorageLoggerMonitor();

		long start = System.currentTimeMillis();

		Instant time = Instant.now();

		long beforePrivateStorages = System.currentTimeMillis();
		StorageMeasurement privateStorages = saveCurrentStatusStorages(time, false, monitor);
		long privateStoragesTime = System.currentTimeMillis() - beforePrivateStorages;

		monitor.setPrivateStoragesTime(privateStoragesTime);
		monitor.setPrivateStoragesCount(monitor.getCommercialStoragesCount());
		monitor.setPrivateQueryTime(monitor.getCommercialQueryTime());
		monitor.setPrivateDatabaseUpdateTime(monitor.getCommercialDatabaseUpdateTime());

		long beforeCommercialStorages = System.currentTimeMillis();
		StorageMeasurement commercialStorages = saveCurrentStatusStorages(time, true, monitor);
		long commercialStoragesTime = System.currentTimeMillis() - beforeCommercialStorages;

		monitor.setCommercialStoragesTime(commercialStoragesTime);

		monitor.setOverallCount(monitor.getCommercialStoragesCount() + monitor.getPrivateStoragesCount());

		long beforeTotalStoragesUpdateDatabase = System.currentTimeMillis();
		influxMeasurementService.saveStorageMeasurement(privateStorages, InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_PRIVATE);
		influxMeasurementService.saveStorageMeasurement(commercialStorages, InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_COMMERCIAL);

		StorageMeasurement totalStorages = new StorageMeasurement();
		totalStorages.setTimestamp(time);
		totalStorages.setDeviceId(null);
		totalStorages.setOwnerId(null);
		totalStorages.setCurrentCharge(privateStorages.getCurrentCharge() + commercialStorages.getCurrentCharge());
		totalStorages.setCapacity(privateStorages.getCapacity() + commercialStorages.getCapacity());
		influxMeasurementService.saveStorageMeasurement(totalStorages, InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_TOTAL);
		long totalStoragesUpdateDatabaseTime = System.currentTimeMillis() - beforeTotalStoragesUpdateDatabase;

		monitor.setTotalStoragesUpdateDatabaseTime(totalStoragesUpdateDatabaseTime);

		monitor.setOverallTime(System.currentTimeMillis() - start);

		monitor.setTimestamp(Instant.now());
		storageLoggerMonitorService.save(monitor);

	}

	public StorageMeasurement saveCurrentStatusStorages(
			Instant time,
			boolean commercial,
			StorageLoggerMonitor monitor
	) {

		String measurementName = commercial ? InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL : InfluxConstants.MEASUREMENT_NAME_STORAGE;
		Set<Long> deviceIds = commercial ? commercialStorageService.getAllDeviceIds() : storageService.getAllDeviceIds();

		long beforeQuery = System.currentTimeMillis();
		Instant start = time.minusSeconds(60 * 60 * 24 * 60);
		String query = String.format("""
        from(bucket: "%s")
          |> range(start: %s, stop: %s)
          |> filter(fn: (r) => r._measurement == "%s")
          |> filter(fn: (r) => r._field == "currentCharge" or r._field == "capacity")
          |> group(columns: ["_field", "deviceId"])
          |> last()
          |> group(columns: ["_field", "ownerId"])
        """, InfluxConstants.BUCKET_STORAGE, start.toString(), time.toString(), measurementName);

		Map<String, Double> chargeOfDevice = new HashMap<>();
		Map<String, Double> capacityOfDevice = new HashMap<>();
		List<FluxTable> tables = influxQueryHelper.executeFluxQuery(query);
		for (FluxTable table : tables) {
			for (FluxRecord record : table.getRecords()) {
				if (deviceIds.contains(Long.valueOf((String) Objects.requireNonNull(record.getValueByKey("deviceId"))))) {
					if (Objects.equals(record.getValueByKey("_field"), "capacity")) {
						capacityOfDevice.merge(
								(String) record.getValueByKey("ownerId"),
								(Double) Objects.requireNonNull(record.getValueByKey("_value")),
								Double::sum
						);
					}
					if (Objects.equals(record.getValueByKey("_field"), "currentCharge")) {
						chargeOfDevice.merge(
								(String) record.getValueByKey("ownerId"),
								(Double) Objects.requireNonNull(record.getValueByKey("_value")),
								Double::sum
						);
					}
				}
			}
		}
		long queryTime = System.currentTimeMillis() - beforeQuery;

		long beforeDatabaseUpdate = System.currentTimeMillis();
		double currentCharge = 0;
		double capacity = 0;

		List<StorageMeasurement> measurements = new ArrayList<>();
		for (Map.Entry<String, Double> entry : chargeOfDevice.entrySet()) {
			StorageMeasurement measurement = new StorageMeasurement();
			measurement.setTimestamp(time);
			measurement.setDeviceId(null);
			measurement.setOwnerId(entry.getKey());
			measurement.setCurrentCharge(entry.getValue());
			measurement.setCapacity(capacityOfDevice.get(entry.getKey()));
			measurements.add(measurement);

			currentCharge += entry.getValue();
			capacity += capacityOfDevice.get(entry.getKey());
		}
		influxMeasurementService.saveStorageMeasurements(measurements, InfluxConstants.MEASUREMENT_NAME_STORAGE_OWNER);

		StorageMeasurement measurement = new StorageMeasurement();
		measurement.setTimestamp(time);
		measurement.setDeviceId(null);
		measurement.setOwnerId(null);
		measurement.setCurrentCharge(currentCharge);
		measurement.setCapacity(capacity);
		long databaseUpdateTime = System.currentTimeMillis() - beforeDatabaseUpdate;

		monitor.setCommercialStoragesCount(measurements.size() + 1);
		monitor.setCommercialQueryTime(queryTime);
		monitor.setCommercialDatabaseUpdateTime(databaseUpdateTime);

		return measurement;
	}
}
