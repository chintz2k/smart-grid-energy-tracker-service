package com.energytracker.influx;

import com.energytracker.influx.measurements.ConsumptionMeasurement;
import com.energytracker.influx.measurements.ProductionMeasurement;
import com.energytracker.influx.measurements.StorageMeasurement;
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
public class InfluxDBService {

	private static final Logger logger = LoggerFactory.getLogger(InfluxDBService.class);

	private final String org = "chintz_de";

	private final String bucketConsumption = "energy_tracker";
	private final String bucketProduction = "energy_tracker";
	private final String bucketStorage = "energy_tracker";

	private final InfluxDBClient influxDBClient;

	@Autowired
	public InfluxDBService(InfluxDBClient influxDBClient) {
		this.influxDBClient = influxDBClient;
	}

	public void saveTotalConsumptionMeasurement(ConsumptionMeasurement measurement) {
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		writeApi.writePoint(bucketConsumption, org, createConsumptionMeasurementPoint(measurement, "consumption_total"));
	}

	public void saveConsumptionMeasurements(List<ConsumptionMeasurement> measurements, @NotNull @NotBlank String measurementName) {
		if (measurements.isEmpty()) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		measurements.forEach(measurement -> writeApi.writePoint(bucketConsumption, org, createConsumptionMeasurementPoint(measurement, measurementName)));
	}

	public void saveProductionMeasurements(List<ProductionMeasurement> measurements, @NotNull @NotBlank String measurementName) {
		if (measurements.isEmpty()) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		measurements.forEach(measurement -> writeApi.writePoint(bucketProduction, org, createProductionMeasurementPoint(measurement, measurementName)));
	}

	public void saveStorageMeasurements(List<StorageMeasurement> measurements, @NotNull @NotBlank String measurementName) {
		if (measurements.isEmpty()) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		measurements.forEach(measurement -> writeApi.writePoint(bucketStorage, org, createStorageMeasurementPoint(measurement, measurementName)));
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

	private Double extractCurrentChargeFromResult(List<FluxTable> results) {
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

	public double getCurrentChargeFromStorage(Long deviceId) {
		// Query für das Measurement "commercial_storages"
		String fluxQueryCommercial = String.format(
				"from(bucket: \"%s\") "
						+ "|> range(start: -30d) "  // Bis zu 30 Tage zurück
						+ "|> filter(fn: (r) => r._measurement == \"storages_commercial\") " // Measurement filtern
						+ "|> filter(fn: (r) => r.deviceId == \"%s\") " // deviceId filtern
						+ "|> filter(fn: (r) => r._field == \"currentCharge\") " // Feld `currentCharge`
						+ "|> last()",  // Nur den letzten Wert auswählen
				bucketStorage, deviceId
		);

		// Query für das Measurement "storages"
		String fluxQueryStorages = String.format(
				"from(bucket: \"%s\") "
						+ "|> range(start: -30d) "
						+ "|> filter(fn: (r) => r._measurement == \"storages\") "
						+ "|> filter(fn: (r) => r.deviceId == \"%s\") "
						+ "|> filter(fn: (r) => r._field == \"currentCharge\") "
						+ "|> last()",
				bucketStorage, deviceId
		);

		try {
			List<FluxTable> commercialResults = influxDBClient.getQueryApi().query(fluxQueryCommercial);
			Double commercialValue = extractCurrentChargeFromResult(commercialResults);
			if (commercialValue != null) {
				return commercialValue; // Erfolgreich gefunden
			}

			// Fallback: Abfrage in "storages"
			List<FluxTable> storageResults = influxDBClient.getQueryApi().query(fluxQueryStorages);
			Double storageValue = extractCurrentChargeFromResult(storageResults);
			if (storageValue != null) {
				return storageValue; // Erfolgreich gefunden
			}

			return 0.0; // Wenn kein Wert gefunden wurde, ist die Ladung 0.0
		} catch (Exception e) {
			logger.error("Fehler bei der Influx Query der Storages, 0.0 zurückgegeben", e);
			return 0.0; // Bei Fehlern einfach 0.0 zurückgeben und den Fehler loggen
		}

	}
}
