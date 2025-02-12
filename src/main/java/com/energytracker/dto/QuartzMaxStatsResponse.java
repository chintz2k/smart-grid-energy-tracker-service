package com.energytracker.dto;

import java.util.Map;

/**
 * @author André Heinen
 */
public class QuartzMaxStatsResponse {

	private Map<String, String> maxStats;

	public QuartzMaxStatsResponse(Map<String, String> maxStats) {
		this.maxStats = maxStats;
	}

	public Map<String, String> getMaxStats() {
		return maxStats;
	}

	public void setMaxStats(Map<String, String> maxStats) {
		this.maxStats = maxStats;
	}
}
