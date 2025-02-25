package com.energytracker.quartz.scheduler;

import com.energytracker.quartz.jobs.consumer.CommercialConsumerLoggerJob;
import com.energytracker.quartz.jobs.consumer.ConsumerLoggerJob;
import com.energytracker.quartz.util.QuartzIntervals;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author André Heinen
 */
@Configuration
public class ConsumerJobScheduler {

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
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.COMMERCIAL_CONSUMER_CRON_JOB))
				.build();
	}

	@Bean
	public JobDetail ConsumerLoggerJobDetail() {
		return JobBuilder.newJob(ConsumerLoggerJob.class)
				.withIdentity("ConsumerLoggerJob", "ConsumptionJobs")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger ConsumerJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(ConsumerLoggerJobDetail())
				.withIdentity("ConsumerLoggerJobTrigger", "ConsumptionJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.CONSUMER_CRON_JOB))
				.build();
	}
}
