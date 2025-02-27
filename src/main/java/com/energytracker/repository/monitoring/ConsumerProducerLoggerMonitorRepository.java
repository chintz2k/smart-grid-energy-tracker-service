package com.energytracker.repository.monitoring;

import com.energytracker.entity.monitoring.ConsumerProducerLoggerMonitor;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author André Heinen
 */
public interface ConsumerProducerLoggerMonitorRepository extends JpaRepository<ConsumerProducerLoggerMonitor, Long> {

}
