package com.energytracker.events;

import com.energytracker.entity.ActiveConsumer;
import com.energytracker.service.ActiveConsumerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * @author André Heinen
 */
@Service
public class ConsumerEventListener {

	private final ActiveConsumerService activeConsumerService;
	private final ObjectMapper objectMapper;

	@Autowired
	public ConsumerEventListener(ActiveConsumerService activeConsumerService, ObjectMapper objectMapper) {
		this.activeConsumerService = activeConsumerService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "consumer-events", groupId = "energy-tracker-group")
	public void processConsumerEvent(String message) throws JsonProcessingException {
		ConsumerEvent consumerEvent = objectMapper.readValue(message, ConsumerEvent.class);

		ActiveConsumer activeConsumer = new ActiveConsumer();
		activeConsumer.setDeviceId(consumerEvent.getDeviceId());
		activeConsumer.setOwnerId(consumerEvent.getOwnerId());
		activeConsumer.setPowerConsumption(consumerEvent.getPowerConsumption());
		activeConsumer.setTimestamp(consumerEvent.getTimestamp());

		boolean commercial = consumerEvent.isCommercial();

		if (consumerEvent.isActive()) {
			if (commercial) {
				activeConsumerService.addCommercial(activeConsumer);
			} else {
				activeConsumerService.addPrivate(activeConsumer);
			}
		} else {
			if (commercial) {
				activeConsumerService.removeCommercial(consumerEvent.getDeviceId());
			} else {
				activeConsumerService.removePrivate(consumerEvent.getDeviceId());
			}
		}
	}
}
