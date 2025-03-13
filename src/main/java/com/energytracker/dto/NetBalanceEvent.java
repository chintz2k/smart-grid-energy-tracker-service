package com.energytracker.dto;

/**
 * @author André Heinen
 */
public class NetBalanceEvent {

	private double currentBalance;
	private double change;

	public NetBalanceEvent() {

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
