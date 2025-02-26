package com.energytracker.service;

import com.energytracker.entity.StorageLoggerMonitor;

/**
 * @author André Heinen
 */
public interface StorageLoggerMonitorService {

	void save(StorageLoggerMonitor stats);

	StorageLoggerMonitor getMaxOverallTime();
}
