package com.energytracker.quartz.jobs.producer;

import com.energytracker.entity.CommercialProducer;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.quartz.util.QuartzIntervals;
import com.energytracker.service.GeneralDeviceService;
import com.energytracker.webclients.WeatherApiClient;

import java.util.List;

/**
 * @author André Heinen
 */
public class CommercialProducerLoggerJob extends AbstractProducerLoggerJob<CommercialProducer> {

	private final GeneralDeviceService<CommercialProducer> commercialProducerService;

	public CommercialProducerLoggerJob(InfluxDBService influxDBService, WeatherApiClient weatherApiClient, GeneralDeviceService<CommercialProducer> commercialProducerService) {
		super(influxDBService, weatherApiClient);
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
		return "commercial_producer_production";
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
