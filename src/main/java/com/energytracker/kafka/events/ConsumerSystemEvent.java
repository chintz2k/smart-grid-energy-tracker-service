package com.energytracker.kafka.events;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class ConsumerSystemEvent {

	private Long deviceId;
	private Long ownerId;
	private double powerConsumption;
	private Instant eventStart;
	private Instant eventEnd;

	public ConsumerSystemEvent() {

	}

	public Long getDeviceId() {
		return deviceId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public double getPowerConsumption() {
		return powerConsumption;
	}

	public Instant getEventStart() {
		return eventStart;
	}

	public Instant getEventEnd() {
		return eventEnd;
	}
}
