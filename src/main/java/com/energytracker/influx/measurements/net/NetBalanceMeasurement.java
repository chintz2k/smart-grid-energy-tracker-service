package com.energytracker.influx.measurements.net;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class NetBalanceMeasurement {

	private Instant timestamp;
	private double currentBalance;
	private double change;

	public NetBalanceMeasurement() {

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
