package com.energytracker.service;

import com.energytracker.entity.StorageLoggerMonitor;
import com.energytracker.repository.StorageLoggerMonitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class StorageLoggerMonitorServiceImpl implements StorageLoggerMonitorService {

	private final StorageLoggerMonitorRepository repository;

	private StorageLoggerMonitor maxOverallTime;

	@Autowired
	public StorageLoggerMonitorServiceImpl(StorageLoggerMonitorRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void save(StorageLoggerMonitor stats) {
		if (maxOverallTime != null) {
			if (stats.getOverallTime() > maxOverallTime.getOverallTime()) {
				maxOverallTime = stats;
			}
		} else {
			maxOverallTime = stats;
		}
		repository.save(stats);
	}

	@Override
	@Transactional(readOnly = true)
	public StorageLoggerMonitor getMaxOverallTime() {
		if (maxOverallTime != null) {
			return maxOverallTime;
		}
		List<StorageLoggerMonitor> stats = repository.findAll();
		if (!stats.isEmpty()) {
			stats.sort(Comparator.comparing(StorageLoggerMonitor::getOverallTime).reversed());
			maxOverallTime = stats.getFirst();
			return maxOverallTime;
		}
		return new StorageLoggerMonitor();
	}
}
