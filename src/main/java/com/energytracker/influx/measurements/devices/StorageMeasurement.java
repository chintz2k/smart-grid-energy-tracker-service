package com.energytracker.influx.measurements.devices;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class StorageMeasurement {

	private Instant timestamp;
	private String deviceId;
	private String ownerId;
	private double capacity;
	private double currentCharge;
	private String commercial;

	public StorageMeasurement() {

	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	public double getCurrentCharge() {
		return currentCharge;
	}

	public void setCurrentCharge(double currentCharge) {
		this.currentCharge = currentCharge;
	}

	public String getCommercial() {
		return commercial;
	}

	public void setCommercial(String commercial) {
		this.commercial = commercial;
	}
}
