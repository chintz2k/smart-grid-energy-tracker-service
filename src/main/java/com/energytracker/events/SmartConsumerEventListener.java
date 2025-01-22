package com.energytracker.events;

import com.energytracker.entity.ActiveSmartConsumer;
import com.energytracker.service.ActiveSmartConsumerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * @author André Heinen
 */
@Service
public class SmartConsumerEventListener {

	private final ActiveSmartConsumerService activeSmartConsumerService;

	@Autowired
	public SmartConsumerEventListener(ActiveSmartConsumerService activeSmartConsumerService) {
		this.activeSmartConsumerService = activeSmartConsumerService;
	}

	@KafkaListener(topics = "smartSmartConsumer-events", groupId = "energy-tracker-group")
	public void processSmartConsumerEvent(String message) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		SmartConsumerEvent smartSmartConsumerEvent = objectMapper.readValue(message, SmartConsumerEvent.class);

		ActiveSmartConsumer activeSmartConsumer = new ActiveSmartConsumer();
		activeSmartConsumer.setDeviceId(smartSmartConsumerEvent.getDeviceId());
		activeSmartConsumer.setOwnerId(smartSmartConsumerEvent.getOwnerId());
		activeSmartConsumer.setPowerConsumption(smartSmartConsumerEvent.getPowerConsumption());
		activeSmartConsumer.setTimestamp(smartSmartConsumerEvent.getTimestamp());

		boolean commercial = smartSmartConsumerEvent.isCommercial();

		if (smartSmartConsumerEvent.isActive()) {
			if (commercial) {
				activeSmartConsumerService.addCommercial(activeSmartConsumer);
			} else {
				activeSmartConsumerService.addPrivate(activeSmartConsumer);
			}
		} else {
			if (commercial) {
				activeSmartConsumerService.removeCommercial(smartSmartConsumerEvent.getDeviceId());
			} else {
				activeSmartConsumerService.removePrivate(smartSmartConsumerEvent.getDeviceId());
			}
		}
	}
}
