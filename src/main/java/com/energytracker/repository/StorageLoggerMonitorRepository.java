package com.energytracker.repository;

import com.energytracker.entity.StorageLoggerMonitor;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author André Heinen
 */
public interface StorageLoggerMonitorRepository extends JpaRepository<StorageLoggerMonitor, Long> {

}
