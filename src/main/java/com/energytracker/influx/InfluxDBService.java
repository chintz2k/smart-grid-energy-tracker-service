package com.energytracker.influx;

import com.energytracker.influx.measurements.ConsumptionMeasurement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 * @author André Heinen
 */
@Service
public class InfluxDBService {

	private final InfluxDBClient influxDBClient;

	@Autowired
	public InfluxDBService(InfluxDBClient influxDBClient) {
		this.influxDBClient = influxDBClient;
	}

	public void saveMeasurements(List<ConsumptionMeasurement> measurements, String measurementName) {
		if (measurements == null || measurements.isEmpty()) {
			return;
		}
		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
		if (measurementName != null) {
			if (measurementName.equals("commercial_consumer_consumption")) {
				measurements.forEach(measurement -> {
					writeApi.writePoint("energy_tracker", "chintz_de", createConsumptionMeasurementPoint(measurement, measurementName));
					writeApi.writePoint("energy_tracker", "chintz_de", createConsumptionMeasurementPoint(measurement, "overall_consumption"));
				});
			} else if (measurementName.equals("commercial_smart_consumer_consumption")) {
				measurements.forEach(measurement -> {
					writeApi.writePoint("energy_tracker", "chintz_de", createConsumptionMeasurementPoint(measurement, measurementName));
					writeApi.writePoint("energy_tracker", "chintz_de", createConsumptionMeasurementPoint(measurement, "overall_consumption"));
				});
			}
		}
	}

	private Point createConsumptionMeasurementPoint(ConsumptionMeasurement measurement, String measurementName) {
		return Point.measurement(measurementName)
				.time(measurement.getTimestamp().toEpochMilli(), WritePrecision.MS)
				.addTag("deviceId", measurement.getDeviceId().toString())
				.addTag("ownerId", measurement.getOwnerId().toString())
				.addField("kWh", measurement.getConsumption());
	}
}
