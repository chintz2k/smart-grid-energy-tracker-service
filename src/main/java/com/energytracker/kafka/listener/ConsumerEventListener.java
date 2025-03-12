package com.energytracker.kafka.listener;

import com.energytracker.entity.devices.CommercialConsumer;
import com.energytracker.entity.devices.Consumer;
import com.energytracker.kafka.events.ConsumerEvent;
import com.energytracker.kafka.events.ConsumerSystemEvent;
import com.energytracker.service.general.GeneralDeviceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class ConsumerEventListener {

	private static final Logger logger = LoggerFactory.getLogger(ConsumerEventListener.class);

	private final GeneralDeviceService<Consumer> consumerService;
	private final GeneralDeviceService<CommercialConsumer> commercialConsumerService;

	private final ObjectMapper objectMapper;

	@Autowired
	public ConsumerEventListener(GeneralDeviceService<Consumer> consumerService, GeneralDeviceService<CommercialConsumer> commercialConsumerService, ObjectMapper objectMapper) {
		this.consumerService = consumerService;
		this.commercialConsumerService = commercialConsumerService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "consumer-events", groupId = "energy-tracker-group")
	public void processConsumerEvent(String message) throws JsonProcessingException {
		ConsumerEvent consumerEvent = objectMapper.readValue(message, ConsumerEvent.class);
		if (consumerEvent.isCommercial()) {
			if (consumerEvent.isActive()) {
				CommercialConsumer commercialConsumer = consumerEvent.toCommercialConsumer();
				commercialConsumerService.add(commercialConsumer);
			} else {
				commercialConsumerService.updateEndTime(consumerEvent.getDeviceId(), consumerEvent.getTimestamp());
			}
		} else {
			if (consumerEvent.isActive()) {
				Consumer consumer = consumerEvent.toConsumer();
				consumerService.add(consumer);
			} else {
				consumerService.updateEndTime(consumerEvent.getDeviceId(), consumerEvent.getTimestamp());
			}
		}
	}

	@KafkaListener(topics = "system-managed-private-consumer-events", groupId = "energy-tracker-group")
	public void processConsumerEventsFromSystem(@Payload String message) throws JsonProcessingException {
		try {
			List<ConsumerSystemEvent> events = objectMapper.readValue(message, new TypeReference<>() {
			});

			for (ConsumerSystemEvent event : events) {
				Consumer consumer = new Consumer();
				consumer.setDeviceId(event.getDeviceId());
				consumer.setOwnerId(event.getOwnerId());
				consumer.setPowerConsumption(event.getPowerConsumption());
				consumer.setStartTime(event.getEventStart());
				consumer.setEndTime(event.getEventEnd());

				consumerService.systemSave(consumer);
			}
		} catch (Exception e) {
			logger.error("Error processing Kafka message: {}", message, e);
			throw e;
		}
	}
}
