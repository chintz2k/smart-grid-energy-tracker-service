package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.CommercialSmartConsumer;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.quartz.QuartzIntervals;
import com.energytracker.service.GeneralDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author André Heinen
 */
@Component
public class CommercialSmartConsumerLoggerJob extends AbstractConsumerLoggerJob<CommercialSmartConsumer> {

	private final GeneralDeviceService<CommercialSmartConsumer> commercialSmartConsumerService;

	@Autowired
	public CommercialSmartConsumerLoggerJob(InfluxDBService influxDBService, GeneralDeviceService<CommercialSmartConsumer> commercialSmartConsumerService) {
		super(influxDBService);
		this.commercialSmartConsumerService = commercialSmartConsumerService;
	}

	@Override
	protected List<CommercialSmartConsumer> getActiveConsumers() {
		return commercialSmartConsumerService.getAll();
	}

	@Override
	protected CommercialSmartConsumer getConsumerById(Long id) {
		return commercialSmartConsumerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return "commercial_smart_consumer_consumption";
	}

	@Override
	protected void updateAll(List<CommercialSmartConsumer> commercialSmartConsumerList) {
		commercialSmartConsumerService.updateAll(commercialSmartConsumerList);
	}

	@Override
	protected void removeAll(List<CommercialSmartConsumer> consumerList) {
		commercialSmartConsumerService.removeAll(consumerList);
	}

	@Override
	protected int getIntervalInSeconds() {
		return QuartzIntervals.COMMERCIAL_SMART_CONSUMER_INTERVAL;
	}
}
