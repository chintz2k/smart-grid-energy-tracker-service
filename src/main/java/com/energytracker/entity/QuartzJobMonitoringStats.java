package com.energytracker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * @author André Heinen
 */
@Entity
public class QuartzJobMonitoringStats {

	@Id
	private Long id;

	// JobKlasse die am längsten dauerte
	private String jobClassName;
	private Long maxExecutionTime = 0L;

	private int activeJobs = 0;
	private int queuedJobs = 0;
	private int maxActiveJobs = 0;
	private int maxQueuedJobs = 0;

	private Instant timestamp;

	public QuartzJobMonitoringStats() {

	}

	public QuartzJobMonitoringStats(Long id) {
		this.id = id;
	}

	// Methode zum Aktualisieren der Maximalwerte
	public void updateMaximums(int newActiveJobs, int newQueuedJobs, long newExecutionTime) {
		this.maxActiveJobs = Math.max(this.maxActiveJobs, newActiveJobs);
		this.maxQueuedJobs = Math.max(this.maxQueuedJobs, newQueuedJobs);
		this.maxExecutionTime = Math.max(this.maxExecutionTime, newExecutionTime);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getJobClassName() {
		return jobClassName;
	}

	public void setJobClassName(String jobClassName) {
		this.jobClassName = jobClassName;
	}

	public Long getMaxExecutionTime() {
		return maxExecutionTime;
	}

	public void setMaxExecutionTime(Long maxExecutionTime) {
		this.maxExecutionTime = maxExecutionTime;
	}

	public int getActiveJobs() {
		return activeJobs;
	}

	public void setActiveJobs(int activeJobs) {
		this.activeJobs = activeJobs;
	}

	public int getQueuedJobs() {
		return queuedJobs;
	}

	public void setQueuedJobs(int queuedJobs) {
		this.queuedJobs = queuedJobs;
	}

	public int getMaxActiveJobs() {
		return maxActiveJobs;
	}

	public void setMaxActiveJobs(int maxActiveJobs) {
		this.maxActiveJobs = maxActiveJobs;
	}

	public int getMaxQueuedJobs() {
		return maxQueuedJobs;
	}

	public void setMaxQueuedJobs(int maxQueuedJobs) {
		this.maxQueuedJobs = maxQueuedJobs;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
}
