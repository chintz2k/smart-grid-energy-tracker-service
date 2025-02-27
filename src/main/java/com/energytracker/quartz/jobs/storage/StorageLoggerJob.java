package com.energytracker.quartz.jobs.storage;

import com.energytracker.entity.devices.CommercialStorage;
import com.energytracker.entity.devices.Storage;
import com.energytracker.entity.monitoring.StorageLoggerMonitor;
import com.energytracker.influx.measurements.devices.StorageMeasurement;
import com.energytracker.influx.service.general.InfluxService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.service.general.GeneralDeviceService;
import com.energytracker.service.monitoring.StorageLoggerMonitorService;
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

	private final InfluxService influxService;
	private final InfluxQueryHelper influxQueryHelper;
	private final GeneralDeviceService<CommercialStorage> commercialStorageService;
	private final GeneralDeviceService<Storage> storageService;
	private final StorageLoggerMonitorService storageLoggerMonitorService;

	@Autowired
	public StorageLoggerJob(InfluxService influxService, InfluxQueryHelper influxQueryHelper, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService, StorageLoggerMonitorService storageLoggerMonitorService) {
		this.influxService = influxService;
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
		if (privateStorages != null) {
			influxService.saveStorageMeasurement(privateStorages, InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_PRIVATE);
		}
		if (commercialStorages != null) {
			influxService.saveStorageMeasurement(commercialStorages, InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_COMMERCIAL);
		}

		if (privateStorages != null || commercialStorages != null) {
			StorageMeasurement totalStorages = new StorageMeasurement();
			totalStorages.setTimestamp(time);
			totalStorages.setDeviceId(null);
			totalStorages.setOwnerId(null);
			double privateCharge = privateStorages != null ? privateStorages.getCurrentCharge() : 0;
			double commercialCharge = commercialStorages != null ? commercialStorages.getCurrentCharge() : 0;
			totalStorages.setCurrentCharge(privateCharge + commercialCharge);
			double privateCapacity = privateStorages != null ? privateStorages.getCapacity() : 0;
			double commercialCapacity = commercialStorages != null ? commercialStorages.getCapacity() : 0;
			totalStorages.setCapacity(privateCapacity + commercialCapacity);
			influxService.saveStorageMeasurement(totalStorages, InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_TOTAL);
		}

		if (privateStorages != null || commercialStorages != null) {
			long totalStoragesUpdateDatabaseTime = System.currentTimeMillis() - beforeTotalStoragesUpdateDatabase;
			monitor.setTotalStoragesUpdateDatabaseTime(totalStoragesUpdateDatabaseTime);
			monitor.setOverallTime(System.currentTimeMillis() - start);
			monitor.setTimestamp(Instant.now());
			storageLoggerMonitorService.save(monitor);
		}
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
		influxService.saveStorageMeasurements(measurements, InfluxConstants.MEASUREMENT_NAME_STORAGE_OWNER);

		StorageMeasurement measurement = null;
		if (!measurements.isEmpty()) {
			measurement = new StorageMeasurement();
			measurement.setTimestamp(time);
			measurement.setDeviceId(null);
			measurement.setOwnerId(null);
			measurement.setCurrentCharge(currentCharge);
			measurement.setCapacity(capacity);
		}
		long databaseUpdateTime = System.currentTimeMillis() - beforeDatabaseUpdate;

		monitor.setCommercialStoragesCount(measurements.size() + measurements.size() > 0 ? 1 : 0);
		monitor.setCommercialQueryTime(queryTime);
		monitor.setCommercialDatabaseUpdateTime(databaseUpdateTime);

		return measurement;
	}
}
