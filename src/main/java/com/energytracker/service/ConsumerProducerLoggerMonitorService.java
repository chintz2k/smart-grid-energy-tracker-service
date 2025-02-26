package com.energytracker.service;

import com.energytracker.entity.ConsumerProducerLoggerMonitor;

/**
 * @author André Heinen
 */
public interface ConsumerProducerLoggerMonitorService {

	void save(ConsumerProducerLoggerMonitor stats);

	ConsumerProducerLoggerMonitor getMaxOverallTime();

}
