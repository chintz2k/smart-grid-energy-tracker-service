package com.energytracker.quartz.jobs.producer;

import com.energytracker.entity.Producer;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.quartz.util.QuartzIntervals;
import com.energytracker.service.GeneralDeviceService;
import com.energytracker.webclients.WeatherApiClient;

import java.util.List;

/**
 * @author André Heinen
 */
public class ProducerLoggerJob extends AbstractProducerLoggerJob<Producer> {

	private final GeneralDeviceService<Producer> producerService;

	public ProducerLoggerJob(InfluxDBService influxDBService, WeatherApiClient weatherApiClient, GeneralDeviceService<Producer> producerService) {
		super(influxDBService, weatherApiClient);
		this.producerService = producerService;
	}

	@Override
	protected List<Producer> getActiveProducers() {
		return producerService.getAll();
	}

	@Override
	protected Producer getProducerById(Long id) {
		return producerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return "producer_production";
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
