package com.energytracker.dto;

/**
 * @author André Heinen
 */
public class WeatherResponse {

	private double solarPower;
	private double windPower;

	public WeatherResponse() {

	}

	public double getSolarPower() {
		return solarPower;
	}

	public void setSolarPower(double solarPower) {
		this.solarPower = solarPower;
	}

	public double getWindPower() {
		return windPower;
	}

	public void setWindPower(double windPower) {
		this.windPower = windPower;
	}
}
