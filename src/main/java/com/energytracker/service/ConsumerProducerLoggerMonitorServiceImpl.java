package com.energytracker.service;

import com.energytracker.entity.ConsumerProducerLoggerMonitor;
import com.energytracker.repository.ConsumerProducerLoggerMonitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class ConsumerProducerLoggerMonitorServiceImpl implements ConsumerProducerLoggerMonitorService {

	private final ConsumerProducerLoggerMonitorRepository repository;

	private ConsumerProducerLoggerMonitor maxOverallTime;

	@Autowired
	public ConsumerProducerLoggerMonitorServiceImpl(ConsumerProducerLoggerMonitorRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void save(ConsumerProducerLoggerMonitor stats) {
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
	public ConsumerProducerLoggerMonitor getMaxOverallTime() {
		if (maxOverallTime != null) {
			return maxOverallTime;
		}
		List<ConsumerProducerLoggerMonitor> stats = repository.findAll();
		if (!stats.isEmpty()) {
			stats.sort(Comparator.comparing(ConsumerProducerLoggerMonitor::getOverallTime).reversed());
			maxOverallTime = stats.getFirst();
			return maxOverallTime;
		}
		return new ConsumerProducerLoggerMonitor();
	}
}
