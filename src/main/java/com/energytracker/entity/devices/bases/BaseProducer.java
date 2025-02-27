package com.energytracker.entity.devices.bases;

import jakarta.persistence.MappedSuperclass;

/**
 * @author André Heinen
 */
@MappedSuperclass
public abstract class BaseProducer extends BaseDevice {

	private double powerProduction;
	private String powerType;
	private boolean renewable;

	public BaseProducer() {

	}

	public double getPowerProduction() {
		return powerProduction;
	}

	public void setPowerProduction(double powerProduction) {
		this.powerProduction = powerProduction;
	}

	public String getPowerType() {
		return powerType;
	}

	public void setPowerType(String powerType) {
		this.powerType = powerType;
	}

	public boolean isRenewable() {
		return renewable;
	}

	public void setRenewable(boolean renewable) {
		this.renewable = renewable;
	}
}
