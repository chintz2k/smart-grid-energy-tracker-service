package com.energytracker.events;

import com.energytracker.entity.CommercialConsumer;
import com.energytracker.entity.Consumer;
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
public class ConsumerEventListener {

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
}
