package com.energytracker.quartz.scheduler;

import com.energytracker.quartz.jobs.netbalance.PowerPlantLimitJob;
import com.energytracker.quartz.util.QuartzIntervals;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author André Heinen
 */
@Configuration
public class PowerPlantLimitScheduler {

	@Bean
	public JobDetail powerPlantLimitJobDetail() {
		return JobBuilder.newJob(PowerPlantLimitJob.class)
				.withIdentity("powerPlantLimitJob", "netBalanceJobs")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger powerPlantLimitJobTrigger() {
		return TriggerBuilder.newTrigger()
				.forJob(powerPlantLimitJobDetail())
				.withIdentity("powerPlantLimitJobTrigger", "netBalanceJobs")
				.withSchedule(CronScheduleBuilder.cronSchedule(QuartzIntervals.NEW_POWER_PLANT_LIMIT_CRON_JOB))
				.build();
	}
}
