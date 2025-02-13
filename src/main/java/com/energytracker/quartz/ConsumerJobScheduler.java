package com.energytracker.quartz;

import com.energytracker.quartz.jobs.consumer.CommercialConsumerLoggerJob;
import com.energytracker.quartz.jobs.consumer.CommercialSmartConsumerLoggerJob;
import com.energytracker.quartz.jobs.consumer.ConsumerLoggerJob;
import com.energytracker.quartz.jobs.consumer.SmartConsumerLoggerJob;
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
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.COMMERCIAL_SMART_CONSUMER_CRON_JOB))
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

	@Bean
	public JobDetail SmartConsumerLoggerJobDetail() {
		return JobBuilder.newJob(SmartConsumerLoggerJob.class)
				.withIdentity("SmartConsumerLoggerJob", "ConsumptionJobs")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger SmartConsumerJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(SmartConsumerLoggerJobDetail())
				.withIdentity("SmartConsumerLoggerJobTrigger", "ConsumptionJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.SMART_CONSUMER_CRON_JOB))
				.build();
	}
}
