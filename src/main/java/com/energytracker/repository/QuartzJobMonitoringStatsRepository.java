package com.energytracker.repository;

import com.energytracker.entity.QuartzJobMonitoringStats;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author André Heinen
 */
public interface QuartzJobMonitoringStatsRepository extends JpaRepository<QuartzJobMonitoringStats, Long> {

}
