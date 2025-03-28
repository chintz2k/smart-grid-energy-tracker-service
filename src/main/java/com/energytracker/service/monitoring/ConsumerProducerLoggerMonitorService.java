package com.energytracker.service.monitoring;

import com.energytracker.entity.monitoring.ConsumerProducerLoggerMonitor;

import java.util.Map;

/**
 * @author André Heinen
 */
public interface ConsumerProducerLoggerMonitorService {

	void save(ConsumerProducerLoggerMonitor stats);

	ConsumerProducerLoggerMonitor getMaxOverallTime();

	Map<String, String> removeOldStats();

}
