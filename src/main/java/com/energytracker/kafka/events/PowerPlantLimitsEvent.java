package com.energytracker.kafka.events;

/**
 * @author André Heinen
 */
public class PowerPlantLimitsEvent {

	private double fossilLimit;
	private double renewableLimit;

	public PowerPlantLimitsEvent() {

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
