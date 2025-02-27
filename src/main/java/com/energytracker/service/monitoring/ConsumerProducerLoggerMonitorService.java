package com.energytracker.service.monitoring;

import com.energytracker.entity.monitoring.ConsumerProducerLoggerMonitor;

/**
 * @author André Heinen
 */
public interface ConsumerProducerLoggerMonitorService {

	void save(ConsumerProducerLoggerMonitor stats);

	ConsumerProducerLoggerMonitor getMaxOverallTime();

}
