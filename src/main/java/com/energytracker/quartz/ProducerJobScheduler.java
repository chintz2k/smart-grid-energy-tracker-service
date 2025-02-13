package com.energytracker.quartz;

import com.energytracker.quartz.jobs.producer.CommercialProducerLoggerJob;
import com.energytracker.quartz.jobs.producer.ProducerLoggerJob;
import com.energytracker.quartz.util.QuartzIntervals;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author André Heinen
 */
@Configuration
public class ProducerJobScheduler {

	@Bean
	public JobDetail commercialProducerLoggerJobDetail() {
		return JobBuilder.newJob(CommercialProducerLoggerJob.class)
				.withIdentity("commercialProducerLoggerJob", "productionJobs")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger commercialProducerJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(commercialProducerLoggerJobDetail())
				.withIdentity("commercialProducerLoggerJobTrigger", "productionJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.COMMERCIAL_PRODUCER_CRON_JOB))
				.build();
	}

	@Bean
	public JobDetail producerLoggerJobDetail() {
		return JobBuilder.newJob(ProducerLoggerJob.class)
				.withIdentity("producerLoggerJob", "productionJobs")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger producerJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(producerLoggerJobDetail())
				.withIdentity("producerLoggerJobTrigger", "productionJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.PRODUCER_CRON_JOB))
				.build();
	}
}
