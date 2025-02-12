package com.energytracker.service;

import com.energytracker.dto.QuartzCurrentStatsResponse;
import com.energytracker.dto.QuartzMaxStatsResponse;

/**
 * @author André Heinen
 */
public interface QuartzJobMonitoringService {

	void updateCurrentStats(int activeJobs, int queuedJobs);
	void updateMaxStats(int activeJobs, int queuedJobs, long executionTime, String className);

	QuartzCurrentStatsResponse getCurrentStats();
	QuartzMaxStatsResponse getMaxStats();

}
