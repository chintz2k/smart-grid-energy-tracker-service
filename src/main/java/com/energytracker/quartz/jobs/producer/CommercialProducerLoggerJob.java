package com.energytracker.quartz.jobs.producer;

import com.energytracker.entity.CommercialProducer;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.quartz.util.QuartzIntervals;
import com.energytracker.quartz.util.StorageHandler;
import com.energytracker.service.GeneralDeviceService;
import com.energytracker.webclients.WeatherApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author André Heinen
 */
@Component
public class CommercialProducerLoggerJob extends AbstractProducerLoggerJob<CommercialProducer> {

	private final GeneralDeviceService<CommercialProducer> commercialProducerService;

	@Autowired
	public CommercialProducerLoggerJob(InfluxDBService influxDBService, WeatherApiClient weatherApiClient, StorageHandler storageHandler, GeneralDeviceService<CommercialProducer> commercialProducerService) {
		super(influxDBService, weatherApiClient, storageHandler);
		this.commercialProducerService = commercialProducerService;
	}

	@Override
	protected List<CommercialProducer> getActiveProducers() {
		return commercialProducerService.getAll();
	}

	@Override
	protected CommercialProducer getProducerById(Long id) {
		return commercialProducerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return "production_device_commercial";
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
