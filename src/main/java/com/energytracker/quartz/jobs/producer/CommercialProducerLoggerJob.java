package com.energytracker.quartz.jobs.producer;

import com.energytracker.entity.devices.CommercialProducer;
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

import java.time.Instant;
import java.util.List;

/**
 * @author André Heinen
 */
@Component
public class CommercialProducerLoggerJob extends AbstractProducerLoggerJob<CommercialProducer> {

	private final GeneralDeviceService<CommercialProducer> commercialProducerService;

	@Autowired
	public CommercialProducerLoggerJob(InfluxService influxService, WeatherApiClient weatherApiClient, StorageHandler storageHandler, PowerPlantLimitsService powerPlantLimitsService, ConsumerProducerLoggerMonitorService consumerProducerLoggerMonitorService, GeneralDeviceService<CommercialProducer> commercialProducerService) {
		super(influxService, weatherApiClient, storageHandler, powerPlantLimitsService, consumerProducerLoggerMonitorService);
		this.commercialProducerService = commercialProducerService;
	}

	@Override
	protected List<CommercialProducer> getActiveProducers(Instant startTime) {
		return commercialProducerService.getByStartTimeBefore(startTime);
	}

	@Override
	protected boolean commercial() {
		return true;
	}

	@Override
	protected CommercialProducer getProducerById(Long id) {
		return commercialProducerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return InfluxConstants.MEASUREMENT_NAME_PRODUCTION_DEVICE;
	}

	@Override
	protected void updateAll(List<CommercialProducer> producerList) {
		commercialProducerService.updateAll(producerList);
	}

	@Override
	protected void removeAll(List<CommercialProducer> producerList) {
		commercialProducerService.removeAll(producerList);
	}

	@Override
	protected int getIntervalInSeconds() {
		return QuartzIntervals.COMMERCIAL_PRODUCER_INTERVAL;
	}
}
