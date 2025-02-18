package com.energytracker.dto;

/**
 * @author André Heinen
 */
public class ProductionDetails {

	private String powerType;
	private boolean renewable;
	private double production;

	public ProductionDetails() {

	}

	public ProductionDetails(String powerType, boolean renewable, double production) {
		this.powerType = powerType;
		this.renewable = renewable;
		this.production = production;
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

	public double getProduction() {
		return production;
	}

	public void setProduction(double production) {
		this.production = production;
	}
}