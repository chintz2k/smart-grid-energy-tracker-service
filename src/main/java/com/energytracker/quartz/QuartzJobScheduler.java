package com.energytracker.quartz;

import com.energytracker.quartz.jobs.consumer.CommercialConsumerLoggerJob;
import com.energytracker.quartz.jobs.consumer.CommercialSmartConsumerLoggerJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author André Heinen
 */
@Configuration
public class QuartzJobScheduler {

	@Bean
	public JobDetail commercialConsumerLoggerJobDetail() {
		return JobBuilder.newJob(CommercialConsumerLoggerJob.class)
				.withIdentity("commercialConsumerLoggerJob", "ConsumptionJobs")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger commercialConsumerJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(commercialConsumerLoggerJobDetail())
				.withIdentity("commercialConsumerLoggerJobTrigger", "ConsumptionJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.CONSUMER_CRON_JOB))
				.build();
	}

	@Bean
	public JobDetail commercialSmartConsumerLoggerJobDetail() {
		return JobBuilder.newJob(CommercialSmartConsumerLoggerJob.class)
				.withIdentity("commercialSmartConsumerLoggerJob", "ConsumptionJobs")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger commercialSmartConsumerJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(commercialSmartConsumerLoggerJobDetail())
				.withIdentity("commercialSmartConsumerLoggerJobTrigger", "ConsumptionJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.SMART_CONSUMER_CRON_JOB))
				.build();
	}
}
