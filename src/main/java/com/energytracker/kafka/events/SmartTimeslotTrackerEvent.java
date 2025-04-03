package com.energytracker.kafka.events;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class SmartTimeslotTrackerEvent {

	private Long timeslotId;
	private Long energyTrackerid;

	private Long deviceId;
	private Long ownerId;
	private double powerConsumption;
	private Instant eventStart;
	private Instant eventEnd;

	public SmartTimeslotTrackerEvent() {

	}

	public Long getTimeslotId() {
		return timeslotId;
	}

	public void setTimeslotId(Long timeslotId) {
		this.timeslotId = timeslotId;
	}

	public Long getEnergyTrackerid() {
		return energyTrackerid;
	}

	public void setEnergyTrackerid(Long energyTrackerid) {
		this.energyTrackerid = energyTrackerid;
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

	public double getPowerConsumption() {
		return powerConsumption;
	}

	public void setPowerConsumption(double powerConsumption) {
		this.powerConsumption = powerConsumption;
	}

	public Instant getEventStart() {
		return eventStart;
	}

	public void setEventStart(Instant eventStart) {
		this.eventStart = eventStart;
	}

	public Instant getEventEnd() {
		return eventEnd;
	}

	public void setEventEnd(Instant eventEnd) {
		this.eventEnd = eventEnd;
	}
}
