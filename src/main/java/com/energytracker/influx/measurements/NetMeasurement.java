package com.energytracker.influx.measurements;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class NetMeasurement {

	private Instant timestamp;
	private double currentBalance;
	private double change;

	public NetMeasurement() {

	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public double getCurrentBalance() {
		return currentBalance;
	}

	public void setCurrentBalance(double currentBalance) {
		this.currentBalance = currentBalance;
	}

	public double getChange() {
		return change;
	}

	public void setChange(double change) {
		this.change = change;
	}
}
