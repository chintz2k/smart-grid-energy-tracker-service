package com.energytracker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * @author André Heinen
 */
@Entity
public class ActiveStorage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long deviceId;
	private Long ownerId;

	private double capacity;
	private double currentCharge;

	private int chargingPriority;
	private int consumingPriority;

	private LocalDateTime timestamp;

	public ActiveStorage() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(Long deviceId) {
		this.deviceId = deviceId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
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

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}
