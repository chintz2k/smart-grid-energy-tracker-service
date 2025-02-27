package com.energytracker.influx.measurements.devices;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class ConsumptionMeasurement {

	private Instant timestamp;
	private Long deviceId;
	private Long ownerId;
	private double consumption;

	public ConsumptionMeasurement() {

	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
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

	public double getConsumption() {
		return consumption;
	}

	public void setConsumption(double consumption) {
		this.consumption = consumption;
	}
}
