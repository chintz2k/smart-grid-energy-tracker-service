package com.energytracker.entity.monitoring;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * @author André Heinen
 */
@Entity
public class ConsumerProducerLoggerMonitor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Instant timestamp;
	private String jobClassName;
	private long readActiveDevicesTime;
	private long processMethodTime;
	private long cpuIntensiveTime;
	private long updateStoragesTime;
	private long updateDatabaseTime;
	private long webRequestsTime;
	private long overallTime;
	private int devicesStartedCount;
	private int devicesUpdatedCount;
	private int devicesRemovedCount;
	private int influxMeasurementsCount;

	public ConsumerProducerLoggerMonitor() {

	}

	public ConsumerProducerLoggerMonitor(
			Instant timestamp,
			String jobClassName,
			long readActiveDevicesTime,
			long processMethodTime,
			long cpuIntensiveTime,
			long updateStoragesTime,
			long updateDatabaseTime,
			long webRequestsTime,
			long overallTime,
			int devicesStartedCount,
			int devicesUpdatedCount,
			int devicesRemovedCount,
			int influxMeasurementsCount
	) {
		this.timestamp = timestamp;
		this.jobClassName = jobClassName;
		this.readActiveDevicesTime = readActiveDevicesTime;
		this.processMethodTime = processMethodTime;
		this.cpuIntensiveTime = cpuIntensiveTime;
		this.updateStoragesTime = updateStoragesTime;
		this.updateDatabaseTime = updateDatabaseTime;
		this.webRequestsTime = webRequestsTime;
		this.overallTime = overallTime;
		this.devicesStartedCount = devicesStartedCount;
		this.devicesUpdatedCount = devicesUpdatedCount;
		this.devicesRemovedCount = devicesRemovedCount;
		this.influxMeasurementsCount = influxMeasurementsCount;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public String getJobClassName() {
		return jobClassName;
	}

	public void setJobClassName(String jobClassName) {
		this.jobClassName = jobClassName;
	}

	public long getReadActiveDevicesTime() {
		return readActiveDevicesTime;
	}

	public void setReadActiveDevicesTime(long readActiveDevicesTime) {
		this.readActiveDevicesTime = readActiveDevicesTime;
	}

	public long getProcessMethodTime() {
		return processMethodTime;
	}

	public void setProcessMethodTime(long processMethodTime) {
		this.processMethodTime = processMethodTime;
	}

	public long getCpuIntensiveTime() {
		return cpuIntensiveTime;
	}

	public void setCpuIntensiveTime(long cpuIntensiveTime) {
		this.cpuIntensiveTime = cpuIntensiveTime;
	}

	public long getUpdateStoragesTime() {
		return updateStoragesTime;
	}

	public void setUpdateStoragesTime(long updateStoragesTime) {
		this.updateStoragesTime = updateStoragesTime;
	}

	public long getUpdateDatabaseTime() {
		return updateDatabaseTime;
	}

	public void setUpdateDatabaseTime(long updateDatabaseTime) {
		this.updateDatabaseTime = updateDatabaseTime;
	}

	public long getWebRequestsTime() {
		return webRequestsTime;
	}

	public void setWebRequestsTime(long webRequestsTime) {
		this.webRequestsTime = webRequestsTime;
	}

	public long getOverallTime() {
		return overallTime;
	}

	public void setOverallTime(long overallTime) {
		this.overallTime = overallTime;
	}

	public int getDevicesStartedCount() {
		return devicesStartedCount;
	}

	public void setDevicesStartedCount(int devicesStartedCount) {
		this.devicesStartedCount = devicesStartedCount;
	}

	public int getDevicesUpdatedCount() {
		return devicesUpdatedCount;
	}

	public void setDevicesUpdatedCount(int devicesUpdatedCount) {
		this.devicesUpdatedCount = devicesUpdatedCount;
	}

	public int getDevicesRemovedCount() {
		return devicesRemovedCount;
	}

	public void setDevicesRemovedCount(int devicesRemovedCount) {
		this.devicesRemovedCount = devicesRemovedCount;
	}

	public int getInfluxMeasurementsCount() {
		return influxMeasurementsCount;
	}

	public void setInfluxMeasurementsCount(int influxMeasurementsCount) {
		this.influxMeasurementsCount = influxMeasurementsCount;
	}
}
