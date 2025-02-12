package com.energytracker.service;

import com.energytracker.dto.QuartzCurrentStatsResponse;
import com.energytracker.dto.QuartzMaxStatsResponse;
import com.energytracker.entity.QuartzJobMonitoringStats;
import com.energytracker.repository.QuartzJobMonitoringStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class QuartzJobMonitoringServiceImpl implements QuartzJobMonitoringService {

	private final QuartzJobMonitoringStatsRepository repository;

	@Autowired
	public QuartzJobMonitoringServiceImpl(QuartzJobMonitoringStatsRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void updateCurrentStats(int activeJobs, int queuedJobs) {
		QuartzJobMonitoringStats currentStats = repository.findById(1L).orElse(new QuartzJobMonitoringStats(1L));
		currentStats.setActiveJobs(activeJobs);
		currentStats.setQueuedJobs(queuedJobs);
		currentStats.setTimestamp(Instant.now());
		repository.save(currentStats);
	}

	@Override
	@Transactional
	public void updateMaxStats(int activeJobs, int queuedJobs, long executionTime, String className) {
		QuartzJobMonitoringStats maxStats = repository.findById(2L).orElse(new QuartzJobMonitoringStats(2L));

		// Aktualisiere maximale aktivierte und wartende Jobs
		boolean isNewMaxExecutionTime = executionTime > maxStats.getMaxExecutionTime();
		maxStats.updateMaximums(activeJobs, queuedJobs, executionTime);

		// Aktualisiere den Job-Namen nur, wenn ein neuer Maximalwert gefunden wurde
		if (isNewMaxExecutionTime) {
			maxStats.setJobClassName(className);
		}

		repository.save(maxStats);
	}

	@Override
	@Transactional(readOnly = true)
	public QuartzCurrentStatsResponse getCurrentStats() {
		QuartzJobMonitoringStats currentStats = repository.findById(1L).orElseThrow(() ->
				new IllegalStateException("Aktuelle Job-Statistiken nicht gefunden!")
		);

		// Erstellen der `Map<String, String>` für die DTO.
		Map<String, String> map = Map.of(
				"Active Jobs", Integer.toString(currentStats.getActiveJobs()),
				"Queued Jobs", Integer.toString(currentStats.getQueuedJobs()),
				"Timestamp", currentStats.getTimestamp().toString()
		);
		return new QuartzCurrentStatsResponse(map);
	}

	@Override
	@Transactional(readOnly = true)
	public QuartzMaxStatsResponse getMaxStats() {
		QuartzJobMonitoringStats maxStats = repository.findById(2L).orElseThrow(() ->
				new IllegalStateException("Maximale Job-Statistiken nicht gefunden!")
		);

		// Erstellen der `Map<String, String>` für die DTO.
		Map<String, String> data = Map.of(
				"Max Active Jobs", Integer.toString(maxStats.getMaxActiveJobs()),
				"Max Queued Jobs", Integer.toString(maxStats.getMaxQueuedJobs()),
				"Job with Max Execution Time", maxStats.getJobClassName(),
				"Max Execution Time (ms)", maxStats.getMaxExecutionTime().toString()
		);
		return new QuartzMaxStatsResponse(data);
	}
}
