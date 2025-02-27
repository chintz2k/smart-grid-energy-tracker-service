package com.energytracker.service.monitoring;

import com.energytracker.entity.monitoring.StorageLoggerMonitor;

/**
 * @author André Heinen
 */
public interface StorageLoggerMonitorService {

	void save(StorageLoggerMonitor stats);

	StorageLoggerMonitor getMaxOverallTime();
}
