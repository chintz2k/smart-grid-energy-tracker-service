package com.energytracker.quartz;

import org.springframework.context.annotation.Configuration;

/**
 * @author André Heinen
 */
@Configuration
public class QuartzIntervals {

	public static final int COMMERCIAL_CONSUMER_INTERVAL = 5;
	public static final int COMMERCIAL_SMART_CONSUMER_INTERVAL = 5;
	public static final int COMMERCIAL_PRODUCER_INTERVAL = 5;
	public static final int COMMERCIAL_STORAGE_INTERVAL = 5;

	public static final int CONSUMER_INTERVAL = 5;
	public static final int SMART_CONSUMER_INTERVAL = 5;
	public static final int PRODUCER_INTERVAL = 5;
	public static final int STORAGE_INTERVAL = 5;

	public static final String COMMERCIAL_CONSUMER_CRON_JOB = "0 * * * * ?";
	public static final String COMMERCIAL_SMART_CONSUMER_CRON_JOB = "0 * * * * ?";
	public static final String COMMERCIAL_PRODUCER_CRON_JOB = "0 * * * * ?";
	public static final String COMMERCIAL_STORAGE_CRON_JOB = "0 * * * * ?";

	public static final String CONSUMER_CRON_JOB = "0 * * * * ?";
	public static final String SMART_CONSUMER_CRON_JOB = "0 * * * * ?";
	public static final String PRODUCER_CRON_JOB = "0 * * * * ?";
	public static final String STORAGE_CRON_JOB = "0 * * * * ?";

}
