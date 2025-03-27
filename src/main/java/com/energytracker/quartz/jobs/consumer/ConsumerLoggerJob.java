package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.devices.Consumer;
import com.energytracker.influx.service.general.InfluxService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.quartz.util.QuartzIntervals;
import com.energytracker.quartz.util.StorageHandler;
import com.energytracker.service.general.GeneralDeviceService;
import com.energytracker.service.monitoring.ConsumerProducerLoggerMonitorService;
import com.energytracker.webclients.DeviceApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * @author André Heinen
 */
@Component
public class ConsumerLoggerJob extends AbstractConsumerLoggerJob<Consumer> {

	private final GeneralDeviceService<Consumer> consumerService;

	@Autowired
	public ConsumerLoggerJob(InfluxService influxService, StorageHandler storageHandler, ConsumerProducerLoggerMonitorService consumerProducerLoggerMonitorService, DeviceApiClient deviceApiClient, GeneralDeviceService<Consumer> consumerService) {
		super(influxService, storageHandler, consumerProducerLoggerMonitorService, deviceApiClient);
		this.consumerService = consumerService;
	}

	@Override
	protected List<Consumer> getActiveConsumers(Instant startTime) {
		return consumerService.getByStartTimeBefore(startTime);
	}

	@Override
	protected Consumer getConsumerById(Long id) {
		return consumerService.getDeviceById(id);
	}

	@Override
	protected String getMeasurementName() {
		return InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_DEVICE;
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
