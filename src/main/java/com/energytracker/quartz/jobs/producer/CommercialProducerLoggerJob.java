package com.energytracker.quartz.jobs.producer;

import com.energytracker.entity.CommercialProducer;
import com.energytracker.entity.CommercialStorage;
import com.energytracker.entity.Storage;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.quartz.util.QuartzIntervals;
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
	public CommercialProducerLoggerJob(InfluxDBService influxDBService, WeatherApiClient weatherApiClient, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService, GeneralDeviceService<CommercialProducer> commercialProducerService) {
		super(influxDBService, weatherApiClient, commercialStorageService, storageService);
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
