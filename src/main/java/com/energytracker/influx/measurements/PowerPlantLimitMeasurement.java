package com.energytracker.influx.measurements;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class PowerPlantLimitMeasurement {

	private Instant timestamp;
	private double limit;

	public PowerPlantLimitMeasurement() {

	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public double getLimit() {
		return limit;
	}

	public void setLimit(double limit) {
		this.limit = limit;
	}
}
