package com.energytracker.kafka.listener;

import com.energytracker.entity.devices.Consumer;
import com.energytracker.kafka.events.SmartTimeslotTrackerEvent;
import com.energytracker.service.general.GeneralDeviceService;
import com.energytracker.webclients.DeviceApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * @author André Heinen
 */
@Service
public class SmartTimeslotEventListener {

	private static final Logger logger = LoggerFactory.getLogger(SmartTimeslotEventListener.class);

	private final GeneralDeviceService<Consumer> service;

	private final ObjectMapper objectMapper;
	private final DeviceApiClient deviceApiClient;

	@Autowired
	public SmartTimeslotEventListener(GeneralDeviceService<Consumer> service, ObjectMapper objectMapper, DeviceApiClient deviceApiClient) {
		this.service = service;
		this.objectMapper = objectMapper;
		this.deviceApiClient = deviceApiClient;
	}

	@KafkaListener(topics = "smart-consumer-timeslot-created", groupId = "energy-tracker-group")
	public void processSmartTimeslotEvent(@Payload String message) {

		SmartTimeslotTrackerEvent event = null;

		try {
			event = objectMapper.readValue(message, SmartTimeslotTrackerEvent.class);
			Consumer consumer = new Consumer();
			consumer.setDeviceId(event.getDeviceId());
			consumer.setOwnerId(event.getOwnerId());
			consumer.setPowerConsumption(event.getPowerConsumption());
			consumer.setStartTime(event.getEventStart());
			consumer.setEndTime(event.getEventEnd());
			Consumer savedConsumer = service.systemSave(consumer);
			respondeMessage(savedConsumer, event.getTimeslotId());
		} catch (JsonProcessingException e) {
			logger.error("Error processing Kafka message: {}", message, e);
		}
	}

	private void respondeMessage(Consumer consumer, Long timeslotId) {
		SmartTimeslotTrackerEvent event = new SmartTimeslotTrackerEvent();
		event.setTimeslotId(timeslotId);
		event.setEnergyTrackerid(consumer.getId());
		event.setDeviceId(consumer.getDeviceId());
		event.setOwnerId(consumer.getOwnerId());
		event.setPowerConsumption(consumer.getPowerConsumption());
		event.setEventStart(consumer.getStartTime());
		event.setEventEnd(consumer.getEndTime());
		try {
			deviceApiClient.updateTimeslotStatus(event);
		} catch (Exception e) {
			logger.error("Error sending message to device: {}", consumer.getDeviceId(), e);
		}
	}

	@KafkaListener(topics = "smart-consumer-timeslot-deleted", groupId = "energy-tracker-group")
	public void processSmartTimeslotDeletedEvent(@Payload String message) {
		try {
			SmartTimeslotTrackerEvent event = objectMapper.readValue(message, SmartTimeslotTrackerEvent.class);
			if (event.getEnergyTrackerid() != null) {
				Consumer consumer = service.getDeviceById(event.getEnergyTrackerid());
				if (consumer != null) {
					service.remove(consumer);
				} else {
					logger.warn("Consumer not found: {}", event.getEnergyTrackerid());
				}
			} else {
				logger.warn("EnergyTrackerId is null");
			}
		} catch (JsonProcessingException e) {
			logger.error("Error processing Kafka message: {}", message, e);
		}
	}
}
