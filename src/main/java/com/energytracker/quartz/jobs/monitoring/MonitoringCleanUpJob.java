package com.energytracker.quartz.jobs.monitoring;

import com.energytracker.service.monitoring.ConsumerProducerLoggerMonitorService;
import com.energytracker.service.monitoring.StorageLoggerMonitorService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author André Heinen
 */
@Component
public class MonitoringCleanUpJob implements Job {

	private static final Logger logger = LoggerFactory.getLogger(MonitoringCleanUpJob.class);

	private final ConsumerProducerLoggerMonitorService consumerProducerLoggerMonitorService;
	private final StorageLoggerMonitorService storageLoggerMonitorService;

	@Autowired
	public MonitoringCleanUpJob(
			ConsumerProducerLoggerMonitorService consumerProducerLoggerMonitorService,
			StorageLoggerMonitorService storageLoggerMonitorService
	) {
		this.consumerProducerLoggerMonitorService = consumerProducerLoggerMonitorService;
		this.storageLoggerMonitorService = storageLoggerMonitorService;
	}

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		try {
			consumerProducerLoggerMonitorService.removeOldStats();
			storageLoggerMonitorService.removeOldStats();
		} catch (Exception e) {
			logger.error("Error while executing monitoring clean up job", e);
			throw new JobExecutionException(e);
		}
	}
}
