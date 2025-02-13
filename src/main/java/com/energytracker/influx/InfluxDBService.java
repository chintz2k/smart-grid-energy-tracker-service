package com.energytracker.influx;

import com.energytracker.influx.measurements.ConsumptionMeasurement;
import com.energytracker.influx.measurements.ProductionMeasurement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 * @author André Heinen
 */
@Service
public class InfluxDBService {

	private final String org = "chintz_de";
	private final String bucketConsumption = "energy_tracker";
	private final String bucketProduction = "energy_tracker";

	private final InfluxDBClient influxDBClient;

	@Autowired
	public InfluxDBService(InfluxDBClient influxDBClient) {
		this.influxDBClient = influxDBClient;
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

	private Point createConsumptionMeasurementPoint(ConsumptionMeasurement measurement, String measurementName) {
		return Point.measurement(measurementName)
				.time(measurement.getTimestamp().toEpochMilli(), WritePrecision.MS)
				.addTag("deviceId", measurement.getDeviceId().toString())
				.addTag("ownerId", measurement.getOwnerId().toString())
				.addField("kWh", measurement.getConsumption());
	}

	private Point createProductionMeasurementPoint(ProductionMeasurement measurement, String measurementName) {
		String renewableString;
		if (measurement.isRenewable()) {
			renewableString = "erneuerbar";
		} else {
			renewableString = "nicht erneuerbar";
		}
		return Point.measurement(measurementName)
				.time(measurement.getTimestamp().toEpochMilli(), WritePrecision.MS)
				.addTag("deviceId", measurement.getDeviceId().toString())
				.addTag("ownerId", measurement.getOwnerId().toString())
				.addTag("powerType", measurement.getPowerType())
				.addTag("renewable", renewableString)
				.addField("kWh", measurement.getProduction());
	}
}
