package com.energytracker.service;

import com.energytracker.dto.QuartzCurrentStatsResponse;
import com.energytracker.dto.QuartzMaxStatsResponse;

import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
public interface QuartzJobMonitoringService {

	void updateCurrentStats(int activeJobs, int queuedJobs);
	void updateMaxStats(int activeJobs, int queuedJobs, long executionTime, String className);

	Map<String, String> resetStats();

	List<String> getRunningJobNames();

	QuartzCurrentStatsResponse getCurrentStats();
	QuartzMaxStatsResponse getMaxStats();

}
