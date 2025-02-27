package com.energytracker.kafka.events;

import com.energytracker.entity.devices.CommercialProducer;
import com.energytracker.entity.devices.Producer;
import com.energytracker.entity.devices.bases.BaseProducer;

import java.time.Instant;

/**
 * @author André Heinen
 */
public class ProducerEvent {

	private Long deviceId;
	private Long ownerId;
	private boolean commercial;
	private boolean active;
	private String powerType;
	private boolean renewable;
	private double powerProduction;
	private Instant timestamp;

	public ProducerEvent() {

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

	public double getPowerProduction() {
		return powerProduction;
	}

	public void setPowerProduction(double powerProduction) {
		this.powerProduction = powerProduction;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	private void populateBaseProducerFields(BaseProducer baseProducer) {
		baseProducer.setDeviceId(deviceId);
		baseProducer.setOwnerId(ownerId);
		baseProducer.setPowerProduction(powerProduction);
		baseProducer.setPowerType(powerType);
		baseProducer.setRenewable(renewable);
		baseProducer.setStartTime(timestamp);
	}

	public Producer toProducer() {
		Producer producer = new Producer();
		populateBaseProducerFields(producer);
		return producer;
	}

	public CommercialProducer toCommercialProducer() {
		CommercialProducer producer = new CommercialProducer();
		populateBaseProducerFields(producer);
		return producer;
	}
}
