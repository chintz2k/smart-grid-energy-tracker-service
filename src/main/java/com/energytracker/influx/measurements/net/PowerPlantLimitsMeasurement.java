package com.energytracker.influx.measurements.net;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class PowerPlantLimitsMeasurement {

	private Instant timestamp;
	private double fossilLimit;
	private double renewableLimit;

	public PowerPlantLimitsMeasurement() {

	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public double getFossilLimit() {
		return fossilLimit;
	}

	public void setFossilLimit(double fossilLimit) {
		this.fossilLimit = fossilLimit;
	}

	public double getRenewableLimit() {
		return renewableLimit;
	}

	public void setRenewableLimit(double renewableLimit) {
		this.renewableLimit = renewableLimit;
	}
}
