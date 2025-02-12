package com.energytracker.events;

import com.energytracker.entity.CommercialSmartConsumer;
import com.energytracker.entity.SmartConsumer;
import com.energytracker.service.GeneralDeviceService;
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

	private final GeneralDeviceService<SmartConsumer> smartConsumerService;
	private final GeneralDeviceService<CommercialSmartConsumer> commercialSmartConsumerService;

	private final ObjectMapper objectMapper;

	@Autowired
	public SmartConsumerEventListener(GeneralDeviceService<SmartConsumer> smartConsumerService, GeneralDeviceService<CommercialSmartConsumer> commercialSmartConsumerService, ObjectMapper objectMapper) {
		this.smartConsumerService = smartConsumerService;
		this.commercialSmartConsumerService = commercialSmartConsumerService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "smart-consumer-events", groupId = "energy-tracker-group")
	public void processSmartConsumerEvent(String message) throws JsonProcessingException {
		SmartConsumerEvent smartConsumerEvent = objectMapper.readValue(message, SmartConsumerEvent.class);
		if (smartConsumerEvent.isCommercial()) {
			if (smartConsumerEvent.isActive()) {
				CommercialSmartConsumer commercialSmartConsumer = smartConsumerEvent.toCommercialSmartConsumer();
				commercialSmartConsumerService.add(commercialSmartConsumer);
			} else {
				commercialSmartConsumerService.updateEndTime(smartConsumerEvent.getDeviceId(), smartConsumerEvent.getTimestamp());
			}
		} else {
			if (smartConsumerEvent.isActive()) {
				SmartConsumer smartConsumer = smartConsumerEvent.toSmartConsumer();
				smartConsumerService.add(smartConsumer);
			} else {
				smartConsumerService.updateEndTime(smartConsumerEvent.getDeviceId(), smartConsumerEvent.getTimestamp());
			}
		}
	}
}
