package com.energytracker.entity;

import jakarta.persistence.MappedSuperclass;

/**
 * @author André Heinen
 */
@MappedSuperclass
public abstract class BaseStorage extends BaseDevice {

	private double capacity;
	private double currentCharge;
	private int chargingPriority;
	private int consumingPriority;

	public BaseStorage() {

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

	public int getChargingPriority() {
		return chargingPriority;
	}

	public void setChargingPriority(int chargingPriority) {
		this.chargingPriority = chargingPriority;
	}

	public int getConsumingPriority() {
		return consumingPriority;
	}

	public void setConsumingPriority(int consumingPriority) {
		this.consumingPriority = consumingPriority;
	}
}
