package com.energytracker.quartz.scheduler;

import com.energytracker.quartz.jobs.storage.StorageLoggerJob;
import com.energytracker.quartz.util.QuartzIntervals;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author André Heinen
 */
@Configuration
public class StorageJobScheduler {

	@Bean
	public JobDetail storageLoggerJobDetail() {
		return JobBuilder.newJob(StorageLoggerJob.class)
				.withIdentity("StorageLoggerJob", "storageJobs")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger storageLoggerJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(storageLoggerJobDetail())
				.withIdentity("StorageLoggerJob", "storageJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.STORAGE_CRON_JOB))
				.build();
	}
}
