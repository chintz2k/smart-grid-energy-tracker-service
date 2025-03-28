package com.energytracker.service.monitoring;

import com.energytracker.entity.monitoring.StorageLoggerMonitor;

import java.util.Map;

/**
 * @author André Heinen
 */
public interface StorageLoggerMonitorService {

	void save(StorageLoggerMonitor stats);

	StorageLoggerMonitor getMaxOverallTime();

	Map<String, String> removeOldStats();

}
