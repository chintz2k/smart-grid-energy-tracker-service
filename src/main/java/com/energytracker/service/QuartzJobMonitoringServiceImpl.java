package com.energytracker.service;

import com.energytracker.dto.QuartzCurrentStatsResponse;
import com.energytracker.dto.QuartzMaxStatsResponse;
import com.energytracker.entity.QuartzJobMonitoringStats;
import com.energytracker.repository.QuartzJobMonitoringStatsRepository;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class QuartzJobMonitoringServiceImpl implements QuartzJobMonitoringService {

	private final Scheduler scheduler;

	private final QuartzJobMonitoringStatsRepository repository;

	@Autowired
	public QuartzJobMonitoringServiceImpl(Scheduler scheduler, QuartzJobMonitoringStatsRepository repository) {
		this.scheduler = scheduler;
		this.repository = repository;
	}

	@Override
	@Transactional
	public synchronized void updateCurrentStats(int activeJobs, int queuedJobs) {
		QuartzJobMonitoringStats currentStats = repository.findById(1L).orElse(new QuartzJobMonitoringStats(1L));
		currentStats.setActiveJobs(activeJobs);
		currentStats.setQueuedJobs(queuedJobs);
		currentStats.setTimestamp(Instant.now());
		repository.save(currentStats);
	}

	@Override
	@Transactional
	public synchronized void updateMaxStats(int activeJobs, int queuedJobs, long executionTime, String className) {
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

	@Override
	@Transactional
	public Map<String, String> resetStats() {
		// Statistiken für currentStats (id = 1) zurücksetzen
		QuartzJobMonitoringStats currentStats = repository.findById(1L).orElse(new QuartzJobMonitoringStats(1L));
		currentStats.setActiveJobs(0);
		currentStats.setQueuedJobs(0);
		currentStats.setTimestamp(Instant.now());
		repository.save(currentStats);

		// Statistiken für maxStats (id = 2) zurücksetzen
		QuartzJobMonitoringStats maxStats = repository.findById(2L).orElse(new QuartzJobMonitoringStats(2L));
		maxStats.setMaxActiveJobs(0);
		maxStats.setMaxQueuedJobs(0);
		maxStats.setMaxExecutionTime(0L);
		maxStats.setJobClassName(null); // Zurücksetzen des Namens der Jobklasse
		repository.save(maxStats);

		Map<String, String> map = Map.of(
				"info", "Alle Stats zurückgesetzt."
		);
		return map;
	}

	@Override
	public List<String> getRunningJobNames() {
		List<String> runningJobs = new ArrayList<>();

		try {
			// Abrufen der Liste der aktuell ausgeführten Jobs
			List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
			for (JobExecutionContext context : currentlyExecutingJobs) {
				runningJobs.add(context.getJobDetail().getKey().getName());
			}
		} catch (SchedulerException e) {
			throw new RuntimeException("Fehler beim Abrufen der laufenden Jobs", e);
		}

		return runningJobs;
	}

}
