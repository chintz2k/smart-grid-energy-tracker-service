package com.energytracker.quartz.jobs.consumer;

import com.energytracker.entity.devices.CommercialConsumer;
import com.energytracker.influx.service.general.InfluxService;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.quartz.util.QuartzIntervals;
import com.energytracker.quartz.util.StorageHandler;
import com.energytracker.service.general.GeneralDeviceService;
import com.energytracker.service.monitoring.ConsumerProducerLoggerMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * @author André Heinen
 */
@Component
public class CommercialConsumerLoggerJob extends AbstractConsumerLoggerJob<CommercialConsumer> {

	private final GeneralDeviceService<CommercialConsumer> commercialConsumerService;

	@Autowired
	public CommercialConsumerLoggerJob(InfluxService influxService, StorageHandler storageHandler, ConsumerProducerLoggerMonitorService consumerProducerLoggerMonitorService, GeneralDeviceService<CommercialConsumer> commercialConsumerService) {
		super(influxService, storageHandler, consumerProducerLoggerMonitorService);
		this.commercialConsumerService = commercialConsumerService;
	}

	@Override
	protected List<CommercialConsumer> getActiveConsumers(Instant startTime) {
		return commercialConsumerService.getByStartTimeBefore(startTime);
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
