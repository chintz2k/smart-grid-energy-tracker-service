package com.energytracker.repository.monitoring;

import com.energytracker.entity.monitoring.StorageLoggerMonitor;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author André Heinen
 */
public interface StorageLoggerMonitorRepository extends JpaRepository<StorageLoggerMonitor, Long> {

}
