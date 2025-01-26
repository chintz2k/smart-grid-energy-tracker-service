package com.energytracker.events;

import com.energytracker.entity.CommercialSmartConsumer;
import com.energytracker.entity.SmartConsumer;
import com.energytracker.service.CommercialSmartConsumerService;
import com.energytracker.service.SmartConsumerService;
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

	private final SmartConsumerService smartConsumerService;
	private final CommercialSmartConsumerService commercialSmartConsumerService;
	private final ObjectMapper objectMapper;

	@Autowired
	public SmartConsumerEventListener(SmartConsumerService smartConsumerService, CommercialSmartConsumerService commercialSmartConsumerService, ObjectMapper objectMapper) {
		this.smartConsumerService = smartConsumerService;
		this.commercialSmartConsumerService = commercialSmartConsumerService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "smart-consumer-events", groupId = "energy-tracker-group")
	public void processSmartConsumerEvent(String message) throws JsonProcessingException {
		SmartConsumerEvent smartSmartConsumerEvent = objectMapper.readValue(message, SmartConsumerEvent.class);
		if (smartSmartConsumerEvent.isCommercial()) {
			if (smartSmartConsumerEvent.isActive()) {
				CommercialSmartConsumer commercialSmartConsumer = new CommercialSmartConsumer();
				commercialSmartConsumer.setDeviceId(smartSmartConsumerEvent.getDeviceId());
				commercialSmartConsumer.setOwnerId(smartSmartConsumerEvent.getOwnerId());
				commercialSmartConsumer.setPowerConsumption(smartSmartConsumerEvent.getPowerConsumption());
				commercialSmartConsumer.setStartTime(smartSmartConsumerEvent.getTimestamp());
				commercialSmartConsumerService.add(commercialSmartConsumer);
			} else {
				CommercialSmartConsumer commercialSmartConsumer = commercialSmartConsumerService.getOpenDeviceByDeviceId(smartSmartConsumerEvent.getDeviceId());
				commercialSmartConsumer.setEndTime(smartSmartConsumerEvent.getTimestamp());
				commercialSmartConsumerService.update(commercialSmartConsumer);
			}
		} else {
			if (smartSmartConsumerEvent.isActive()) {
				SmartConsumer smartConsumer = new SmartConsumer();
				smartConsumer.setDeviceId(smartSmartConsumerEvent.getDeviceId());
				smartConsumer.setOwnerId(smartSmartConsumerEvent.getOwnerId());
				smartConsumer.setPowerConsumption(smartSmartConsumerEvent.getPowerConsumption());
				smartConsumer.setStartTime(smartSmartConsumerEvent.getTimestamp());
				smartConsumerService.add(smartConsumer);
			} else {
				SmartConsumer smartConsumer = smartConsumerService.getOpenDeviceByDeviceId(smartSmartConsumerEvent.getDeviceId());
				smartConsumer.setEndTime(smartSmartConsumerEvent.getTimestamp());
				smartConsumerService.update(smartConsumer);
			}
		}
	}
}
