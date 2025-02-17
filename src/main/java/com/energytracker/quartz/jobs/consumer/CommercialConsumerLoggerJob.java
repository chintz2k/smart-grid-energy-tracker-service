package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.CommercialConsumer;
import com.energytracker.entity.CommercialStorage;
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
public class CommercialConsumerLoggerJob extends AbstractConsumerLoggerJob<CommercialConsumer> {

	private final GeneralDeviceService<CommercialConsumer> commercialConsumerService;

	@Autowired
	public CommercialConsumerLoggerJob(InfluxDBService influxDBService, GeneralDeviceService<CommercialStorage> commercialStorageService, GeneralDeviceService<Storage> storageService, GeneralDeviceService<CommercialConsumer> commercialConsumerService) {
		super(influxDBService, commercialStorageService, storageService);
		this.commercialConsumerService = commercialConsumerService;
	}

	@Override
	protected List<CommercialConsumer> getActiveConsumers() {
		return commercialConsumerService.getAll();
	}

	@Override
	protected CommercialConsumer getConsumerById(Long id) {
		return commercialConsumerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return "commercial_consumer_consumption";
	}

	@Override
	protected void updateAll(List<CommercialConsumer> commercialConsumerList) {
		commercialConsumerService.updateAll(commercialConsumerList);
	}

	@Override
	protected void removeAll(List<CommercialConsumer> commercialConsumerList) {
		commercialConsumerService.removeAll(commercialConsumerList);
	}

	@Override
	protected int getIntervalInSeconds() {
		return QuartzIntervals.COMMERCIAL_CONSUMER_INTERVAL;
	}
}
