package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.SmartConsumer;
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
public class SmartConsumerLoggerJob extends AbstractConsumerLoggerJob<SmartConsumer> {

	private final GeneralDeviceService<SmartConsumer> smartConsumerService;

	@Autowired
	public SmartConsumerLoggerJob(InfluxDBService influxDBService, GeneralDeviceService<SmartConsumer> smartConsumerService) {
		super(influxDBService);
		this.smartConsumerService = smartConsumerService;
	}

	@Override
	protected List<SmartConsumer> getActiveConsumers() {
		return smartConsumerService.getAll();
	}

	@Override
	protected SmartConsumer getConsumerById(Long id) {
		return smartConsumerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return "smart_consumer_consumption";
	}

	@Override
	protected void updateAll(List<SmartConsumer> smartConsumerList) {
		smartConsumerService.updateAll(smartConsumerList);
	}

	@Override
	protected void removeAll(List<SmartConsumer> consumerList) {
		smartConsumerService.removeAll(consumerList);
	}

	@Override
	protected int getIntervalInSeconds() {
		return QuartzIntervals.SMART_CONSUMER_INTERVAL;
	}
}
