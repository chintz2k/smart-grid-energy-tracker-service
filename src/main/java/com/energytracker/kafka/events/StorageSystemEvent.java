package com.energytracker.kafka.events;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class StorageSystemEvent {

	private Long deviceId;
	private Long ownerId;
	private double capacity;
	private int chargingPriority;
	private int consumingPriority;
	private double currentCharge;
	private Instant eventStart;
	private Instant eventEnd;

	public StorageSystemEvent() {

	}

	public Long getDeviceId() {
		return deviceId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public double getCapacity() {
		return capacity;
	}

	public int getChargingPriority() {
		return chargingPriority;
	}

	public int getConsumingPriority() {
		return consumingPriority;
	}

	public double getCurrentCharge() {
		return currentCharge;
	}

	public Instant getEventStart() {
		return eventStart;
	}

	public Instant getEventEnd() {
		return eventEnd;
	}
}
