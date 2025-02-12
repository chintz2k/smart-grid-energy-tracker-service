package com.energytracker.entity;

import jakarta.persistence.MappedSuperclass;

import java.time.Instant;

/**
 * @author André Heinen
 */
@MappedSuperclass
public abstract class BaseDevice {

	private Long deviceId;
	private Long ownerId;
	private Instant startTime;
	private Instant endTime = null;
	private Instant lastUpdate = null;

	public BaseDevice() {

	}

	public Long getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(Long deviceId) {
		this.deviceId = deviceId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public Instant getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Instant lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
