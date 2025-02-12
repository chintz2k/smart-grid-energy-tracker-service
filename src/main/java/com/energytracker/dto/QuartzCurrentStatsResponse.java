package com.energytracker.dto;

import java.util.Map;

/**
 * @author André Heinen
 */
public class QuartzCurrentStatsResponse {

	private Map<String, String> currentStats;

	public QuartzCurrentStatsResponse(Map<String, String> currentStats) {
		this.currentStats = currentStats;
	}

	public Map<String, String> getCurrentStats() {
		return currentStats;
	}

	public void setCurrentStats(Map<String, String> currentStats) {
		this.currentStats = currentStats;
	}
}
