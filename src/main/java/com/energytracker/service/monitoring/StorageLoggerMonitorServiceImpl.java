package com.energytracker.service.monitoring;

import com.energytracker.entity.monitoring.StorageLoggerMonitor;
import com.energytracker.repository.monitoring.StorageLoggerMonitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

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

	@Override
	@Transactional
	public Map<String, String> removeOldStats() {
		Instant limit = Instant.now().minusSeconds(60 * 60 * 24 * 3); // 3 Tage
		List<StorageLoggerMonitor> stats = repository.findAll();
		List<Long> toRemove = new ArrayList<>();
		if (!stats.isEmpty()) {
			// Den größten overallTime Wert suchen
			stats.sort(Comparator.comparing(StorageLoggerMonitor::getOverallTime).reversed());
			maxOverallTime = stats.getFirst();
			for (StorageLoggerMonitor stat : stats) {
				if (stat.getTimestamp().isBefore(limit)) {
					if (!Objects.equals(stat.getId(), maxOverallTime.getId())) {
						toRemove.add(stat.getId());
					}
				}
			}
		}
		int removedEntries = toRemove.size();
		if (removedEntries > 0) {
			repository.deleteAllById(toRemove);
			return Map.of("message", "Successfully removed " + removedEntries + " old entries");
		}
		return Map.of("message", "No old entries to remove");
	}
}
