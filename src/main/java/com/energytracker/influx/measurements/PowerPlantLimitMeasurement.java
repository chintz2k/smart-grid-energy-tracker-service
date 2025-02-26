package com.energytracker.influx.measurements;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class PowerPlantLimitMeasurement {

	private Instant timestamp;
	private double fossilLimit;
	private double renewableLimit;

	public PowerPlantLimitMeasurement() {

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
