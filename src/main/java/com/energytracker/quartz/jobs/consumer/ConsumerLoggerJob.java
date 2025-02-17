package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.CommercialStorage;
import com.energytracker.entity.Consumer;
import com.energytracker.entity.Storage;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.quartz.util.QuartzIntervals;
import com.energytracker.service.GeneralDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author André Heinen
 */
@Component
public class ConsumerLoggerJob extends AbstractConsumerLoggerJob<Consumer> {

	private final GeneralDeviceService<Consumer> consumerService;

	@Autowired
	public ConsumerLoggerJob(InfluxDBService influxDBService, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService, GeneralDeviceService<Consumer> consumerService) {
		super(influxDBService, commercialStorageService, storageService);
		this.consumerService = consumerService;
	}

	@Override
	protected List<Consumer> getActiveConsumers() {
		return consumerService.getAll();
	}

	@Override
	protected Consumer getConsumerById(Long id) {
		return consumerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return "consumer_consumption";
	}

	@Override
	protected void updateAll(List<Consumer> consumerList) {
		consumerService.updateAll(consumerList);
	}

	@Override
	protected void removeAll(List<Consumer> consumerList) {
		consumerService.removeAll(consumerList);
	}

	@Override
	protected int getIntervalInSeconds() {
		return QuartzIntervals.CONSUMER_INTERVAL;
	}
}
