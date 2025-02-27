package com.energytracker.kafka.events;

import com.energytracker.entity.devices.CommercialConsumer;
import com.energytracker.entity.devices.Consumer;
import com.energytracker.entity.devices.bases.BaseConsumer;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class ConsumerEvent {

	private Long deviceId;
	private Long ownerId;
	private boolean commercial;
	private boolean active;
	private double powerConsumption;
	private Instant timestamp;

	public ConsumerEvent() {

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

	private void populateBaseConsumerFields(BaseConsumer baseConsumer) {
		baseConsumer.setDeviceId(deviceId);
		baseConsumer.setOwnerId(ownerId);
		baseConsumer.setPowerConsumption(powerConsumption);
		baseConsumer.setStartTime(timestamp);
	}

	public Consumer toConsumer() {
		Consumer consumer = new Consumer();
		populateBaseConsumerFields(consumer);
		return consumer;
	}

	public CommercialConsumer toCommercialConsumer() {
		CommercialConsumer consumer = new CommercialConsumer();
		populateBaseConsumerFields(consumer);
		return consumer;
	}
}
