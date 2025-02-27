package com.energytracker.kafka.listener;

import com.energytracker.influx.measurements.net.PowerPlantLimitsMeasurement;
import com.energytracker.influx.service.general.InfluxService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.kafka.events.PowerPlantLimitsEvent;
import com.energytracker.service.net.PowerPlantLimitsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * @author André Heinen
 */
@Service
public class PowerPlantLimitsEventListener {

	private final ObjectMapper objectMapper;
	private final InfluxService influxService;
	private final PowerPlantLimitsService powerPlantLimitsService;

	@Autowired
	public PowerPlantLimitsEventListener(ObjectMapper objectMapper, InfluxService influxService, PowerPlantLimitsService powerPlantLimitsService) {
		this.objectMapper = objectMapper;
		this.influxService = influxService;
		this.powerPlantLimitsService = powerPlantLimitsService;
	}

	@KafkaListener(topics = "power-plant-limits", groupId = "energy-tracker-group")
	public void netMeasurementListener(String message) throws JsonProcessingException {
		PowerPlantLimitsEvent powerPlantLimitsEvent = objectMapper.readValue(message, PowerPlantLimitsEvent.class);

		Instant timestamp = Instant.now();
		PowerPlantLimitsMeasurement measurement = new PowerPlantLimitsMeasurement();
		measurement.setTimestamp(timestamp);
		measurement.setFossilLimit(powerPlantLimitsEvent.getFossilLimit());
		measurement.setRenewableLimit(powerPlantLimitsEvent.getRenewableLimit());
		influxService.savePowerPlantLimit(measurement, InfluxConstants.MEASUREMENT_NAME_POWER_PLANT_LIMIT);

		powerPlantLimitsService.setNewCommercialPowerPlantLimit(powerPlantLimitsEvent);
	}
}
