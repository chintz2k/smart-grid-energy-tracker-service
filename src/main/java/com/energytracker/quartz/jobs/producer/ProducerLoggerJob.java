package com.energytracker.quartz.jobs.producer;

import com.energytracker.entity.devices.Producer;
import com.energytracker.influx.service.general.InfluxService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.quartz.util.QuartzIntervals;
import com.energytracker.quartz.util.StorageHandler;
import com.energytracker.service.general.GeneralDeviceService;
import com.energytracker.service.monitoring.ConsumerProducerLoggerMonitorService;
import com.energytracker.service.net.PowerPlantLimitsService;
import com.energytracker.webclients.WeatherApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author André Heinen
 */
@Component
public class ProducerLoggerJob extends AbstractProducerLoggerJob<Producer> {

	private final GeneralDeviceService<Producer> producerService;

	@Autowired
	public ProducerLoggerJob(InfluxService influxService, WeatherApiClient weatherApiClient, StorageHandler storageHandler, PowerPlantLimitsService powerPlantLimitsService, ConsumerProducerLoggerMonitorService consumerProducerLoggerMonitorService, GeneralDeviceService<Producer> producerService) {
		super(influxService, weatherApiClient, storageHandler, powerPlantLimitsService, consumerProducerLoggerMonitorService);
		this.producerService = producerService;
	}

	@Override
	protected List<Producer> getActiveProducers() {
		return producerService.getAll();
	}

	@Override
	protected boolean commercial() {
		return false;
	}

	@Override
	protected Producer getProducerById(Long id) {
		return producerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return InfluxConstants.MEASUREMENT_NAME_PRODUCTION_DEVICE;
	}

	@Override
	protected void updateAll(List<Producer> producerList) {
		producerService.updateAll(producerList);
	}

	@Override
	protected void removeAll(List<Producer> producerList) {
		producerService.removeAll(producerList);
	}

	@Override
	protected int getIntervalInSeconds() {
		return QuartzIntervals.PRODUCER_INTERVAL;
	}
}
