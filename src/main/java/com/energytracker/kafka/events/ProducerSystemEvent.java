package com.energytracker.kafka.events;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class ProducerSystemEvent {

	private Long deviceId;
	private Long ownerId;
	private double powerProduction;
	private String powerType;
	private boolean renewable;
	private Instant eventStart;
	private Instant eventEnd;

	public ProducerSystemEvent() {

	}

	public Long getDeviceId() {
		return deviceId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public double getPowerProduction() {
		return powerProduction;
	}

	public String getPowerType() {
		return powerType;
	}

	public boolean isRenewable() {
		return renewable;
	}

	public Instant getEventStart() {
		return eventStart;
	}

	public Instant getEventEnd() {
		return eventEnd;
	}
}
