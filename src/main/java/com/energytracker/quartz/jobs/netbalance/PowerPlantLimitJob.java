package com.energytracker.quartz.jobs.netbalance;

import com.energytracker.influx.measurements.PowerPlantLimitMeasurement;
import com.energytracker.influx.service.general.InfluxMeasurementService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.service.NetBalanceService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * @author André Heinen
 */
@Component
public class PowerPlantLimitJob implements Job {

	private final NetBalanceService netBalanceService;
	private final InfluxMeasurementService influxMeasurementService;

	@Autowired
	public PowerPlantLimitJob(NetBalanceService netBalanceService, InfluxMeasurementService influxMeasurementService) {
		this.netBalanceService = netBalanceService;
		this.influxMeasurementService = influxMeasurementService;
	}

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		Map<String, Double> powerPlantLimit = netBalanceService.setNewCommercialPowerPlantLimit();

		Instant timestamp = Instant.now();
		PowerPlantLimitMeasurement measurement = new PowerPlantLimitMeasurement();
		measurement.setTimestamp(timestamp);
		measurement.setFossilLimit(powerPlantLimit.get(NetBalanceService.CACHE_KEY_FOR_FOSSIL));
		measurement.setRenewableLimit(powerPlantLimit.get(NetBalanceService.CACHE_KEY_FOR_RENEWABLE));
		influxMeasurementService.savePowerPlantLimit(measurement, InfluxConstants.MEASUREMENT_NAME_POWER_PLANT_LIMIT);
	}
}
