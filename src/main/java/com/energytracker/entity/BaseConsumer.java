package com.energytracker.entity;

import jakarta.persistence.MappedSuperclass;

/**
 * @author André Heinen
 */
@MappedSuperclass
public abstract class BaseConsumer extends BaseDevice {

	private double powerConsumption;

	public BaseConsumer() {

	}

	public double getPowerConsumption() {
		return powerConsumption;
	}

	public void setPowerConsumption(double powerConsumption) {
		this.powerConsumption = powerConsumption;
	}
}
