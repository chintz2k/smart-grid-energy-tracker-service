package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.CommercialConsumer;
import com.energytracker.influx.InfluxConstants;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.quartz.util.QuartzIntervals;
import com.energytracker.quartz.util.StorageHandler;
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
	public CommercialConsumerLoggerJob(InfluxDBService influxDBService, StorageHandler storageHandler, GeneralDeviceService<CommercialConsumer> commercialConsumerService) {
		super(influxDBService, storageHandler);
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
		return InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_DEVICE;
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
