package com.energytracker.events;

import com.energytracker.entity.BaseConsumer;
import com.energytracker.entity.CommercialSmartConsumer;
import com.energytracker.entity.SmartConsumer;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class SmartConsumerEvent {

	private Long deviceId;
	private Long ownerId;
	private boolean commercial;
	private boolean active;
	private double powerConsumption;
	private Instant timestamp;

	public SmartConsumerEvent() {

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

	public boolean isCommercial() {
		return commercial;
	}

	public void setCommercial(boolean commercial) {
		this.commercial = commercial;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public double getPowerConsumption() {
		return powerConsumption;
	}

	public void setPowerConsumption(double powerConsumption) {
		this.powerConsumption = powerConsumption;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	private void populateBaseSmartConsumerFields(BaseConsumer baseConsumer) {
		baseConsumer.setDeviceId(deviceId);
		baseConsumer.setOwnerId(ownerId);
		baseConsumer.setPowerConsumption(powerConsumption);
		baseConsumer.setStartTime(timestamp);
	}

	public SmartConsumer toSmartConsumer() {
		SmartConsumer consumer = new SmartConsumer();
		populateBaseSmartConsumerFields(consumer);
		return consumer;
	}

	public CommercialSmartConsumer toCommercialSmartConsumer() {
		CommercialSmartConsumer consumer = new CommercialSmartConsumer();
		populateBaseSmartConsumerFields(consumer);
		return consumer;
	}
}
