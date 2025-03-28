package com.energytracker.quartz.scheduler;

import com.energytracker.quartz.jobs.monitoring.MonitoringCleanUpJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author André Heinen
 */
@Configuration
public class MonitoringCleanUpScheduler {
	
	@Bean
	public JobDetail monitoringCleanUpJobDetail() {
		return JobBuilder.newJob(MonitoringCleanUpJob.class)
				.withIdentity("MonitoringCleanUpJob", "MonitoringJobs")
				.storeDurably()
				.build();
	}
	
	@Bean
	public Trigger monitoringCleanUpJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(monitoringCleanUpJobDetail())
				.withIdentity("MonitoringCleanUpJobTrigger", "MonitoringJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 6 * * ?"))
				.build();
	}
}
