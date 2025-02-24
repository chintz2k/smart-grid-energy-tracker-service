package com.energytracker.quartz.util;

import org.springframework.context.annotation.Configuration;

/**
 * @author André Heinen
 */
@Configuration
public class QuartzIntervals {

	// TODO Vor Release auf jeden Fall die Intervalle anpassen!

	public static final int COMMERCIAL_CONSUMER_INTERVAL = 5;
	public static final int COMMERCIAL_PRODUCER_INTERVAL = 5;

	public static final int CONSUMER_INTERVAL = 5;
	public static final int PRODUCER_INTERVAL = 5;

	public static final String COMMERCIAL_CONSUMER_CRON_JOB = "0 * * * * ?";
	public static final String COMMERCIAL_PRODUCER_CRON_JOB = "0 * * * * ?";

	public static final String CONSUMER_CRON_JOB = "0 * * * * ?";
	public static final String PRODUCER_CRON_JOB = "0 * * * * ?";

	public static final String STORAGE_CRON_JOB = "3 * * * * ?";

}
