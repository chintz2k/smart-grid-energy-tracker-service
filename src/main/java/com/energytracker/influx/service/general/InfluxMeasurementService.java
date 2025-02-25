package com.energytracker.influx.service.general;

import com.energytracker.influx.measurements.*;
import com.energytracker.influx.util.InfluxConstants;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 * @author André Heinen
 */
@Service
public class InfluxMeasurementService {

	private static final Logger logger = LoggerFactory.getLogger(InfluxMeasurementService.class);

	private final InfluxDBClient influxDBClient;

	@Autowired
	public InfluxMeasurementService(InfluxDBClient influxDBClient) {
		this.influxDBClient = influxDBClient;
	}

	public void saveConsumptionMeasurements(List<ConsumptionMeasurement> measurements, @NotNull @NotBlank String measurementName) {
		if (measurements.isEmpty()) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		measurements.forEach(measurement -> writeApi.writePoint(InfluxConstants.BUCKET_CONSUMPTION, InfluxConstants.ORG_NAME, createConsumptionMeasurementPoint(measurement, measurementName)));
	}

	public void saveProductionMeasurements(List<ProductionMeasurement> measurements, @NotNull @NotBlank String measurementName) {
		if (measurements.isEmpty()) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		measurements.forEach(measurement -> writeApi.writePoint(InfluxConstants.BUCKET_PRODUCTION, InfluxConstants.ORG_NAME, createProductionMeasurementPoint(measurement, measurementName)));
	}

	public void saveStorageMeasurements(List<StorageMeasurement> measurements, @NotNull @NotBlank String measurementName) {
		if (measurements.isEmpty()) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		measurements.forEach(measurement -> writeApi.writePoint(InfluxConstants.BUCKET_STORAGE, InfluxConstants.ORG_NAME, createStorageMeasurementPoint(measurement, measurementName)));
	}

	public void saveStorageMeasurement(StorageMeasurement measurement, @NotNull @NotBlank String measurementName) {
		if (measurement == null) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		writeApi.writePoint(InfluxConstants.BUCKET_STORAGE, InfluxConstants.ORG_NAME, createStorageMeasurementPoint(measurement, measurementName));
	}

	public void saveNetMeasurement(NetMeasurement measurement, @NotNull @NotBlank String measurementName) {
		if (measurement == null) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		writeApi.writePoint(InfluxConstants.BUCKET_NET, InfluxConstants.ORG_NAME, createNetMeasurementPoint(measurement, measurementName));
	}

	public void savePowerPlantLimit(PowerPlantLimitMeasurement measurement, @NotNull @NotBlank String measurementName) {
		if (measurement == null) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		writeApi.writePoint(InfluxConstants.BUCKET_NET, InfluxConstants.ORG_NAME, createPowerPlantLimitPoint(measurement, measurementName));
	}

	private Point createConsumptionMeasurementPoint(ConsumptionMeasurement measurement, String measurementName) {
		String deviceId = null;
		if (measurement.getDeviceId() != null) {
			deviceId = measurement.getDeviceId().toString();
		}
		String ownerId = null;
		if (measurement.getOwnerId() != null) {
			ownerId = measurement.getOwnerId().toString();
		}
		return Point.measurement(measurementName)
				.time(measurement.getTimestamp().toEpochMilli(), WritePrecision.MS)
				.addTag("deviceId", deviceId)
				.addTag("ownerId", ownerId)
				.addField("kWh", measurement.getConsumption());
	}

	private Point createProductionMeasurementPoint(ProductionMeasurement measurement, String measurementName) {
		String deviceId = null;
		if (measurement.getDeviceId() != null) {
			deviceId = measurement.getDeviceId().toString();
		}
		String ownerId = null;
		if (measurement.getOwnerId() != null) {
			ownerId = measurement.getOwnerId().toString();
		}
		String renewableString;
		if (measurement.isRenewable()) {
			renewableString = "erneuerbar";
		} else {
			renewableString = "nicht erneuerbar";
		}
		if (!measurement.getRenewable().equals("true") && !measurement.getRenewable().equals("false")) {
			renewableString = "mixed";
		}
		return Point.measurement(measurementName)
				.time(measurement.getTimestamp().toEpochMilli(), WritePrecision.MS)
				.addTag("deviceId", deviceId)
				.addTag("ownerId", ownerId)
				.addTag("powerType", measurement.getPowerType())
				.addTag("renewable", renewableString)
				.addField("kWh", measurement.getProduction());
	}

	private Point createStorageMeasurementPoint(StorageMeasurement measurement, String measurementName) {
		return Point.measurement(measurementName)
				.time(measurement.getTimestamp().toEpochMilli(), WritePrecision.MS)
				.addTag("deviceId", measurement.getDeviceId())
				.addTag("ownerId", measurement.getOwnerId())
				.addField("capacity", measurement.getCapacity())
				.addField("currentCharge", measurement.getCurrentCharge());
	}

	private Point createNetMeasurementPoint(NetMeasurement measurement, String measurementName) {
		return Point.measurement(measurementName)
				.time(measurement.getTimestamp().toEpochMilli(), WritePrecision.MS)
				.addField("currentBalance", measurement.getCurrentBalance())
				.addField("change", measurement.getChange());
	}

	private Point createPowerPlantLimitPoint(PowerPlantLimitMeasurement measurement, String measurementName) {
		return Point.measurement(measurementName)
				.time(measurement.getTimestamp().toEpochMilli(), WritePrecision.MS)
				.addField("limit", measurement.getLimit());
	}

	public Long getDeviceOwnerId(Long deviceId) {
		List<String> buckets = List.of(
				InfluxConstants.BUCKET_CONSUMPTION,
				InfluxConstants.BUCKET_PRODUCTION,
				InfluxConstants.BUCKET_STORAGE
		);

		for (String bucket : buckets) {
			String fluxQuery = String.format(
					"from(bucket: \"%s\") "
							+ "|> range(start: -1y) "
							+ "|> filter(fn: (r) => r.deviceId == \"%s\") "
							+ "|> keep(columns: [\"ownerId\"]) "
							+ "|> limit(n: 1)",
					bucket, deviceId
			);

			List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);
			if (!tables.isEmpty()) {
				for (FluxTable table : tables) {
					List<FluxRecord> records = table.getRecords();
					if (!records.isEmpty()) {
						Object ownerIdObj = records.getFirst().getValueByKey("ownerId");
						if (ownerIdObj != null) {
							return Long.valueOf(ownerIdObj.toString());
						}
					}
				}
			}
		}
		logger.error("Keine OwnerId für deviceId {} in den Buckets gefunden.", deviceId);
		return null;
	}
}
