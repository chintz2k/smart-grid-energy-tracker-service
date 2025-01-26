package com.energytracker.events;

import com.energytracker.entity.CommercialConsumer;
import com.energytracker.entity.Consumer;
import com.energytracker.service.CommercialConsumerService;
import com.energytracker.service.ConsumerService;
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

	private final ConsumerService consumerService;
	private final CommercialConsumerService commercialConsumerService;
	private final ObjectMapper objectMapper;

	@Autowired
	public ConsumerEventListener(ConsumerService consumerService, CommercialConsumerService commercialConsumerService, ObjectMapper objectMapper) {
		this.consumerService = consumerService;
		this.commercialConsumerService = commercialConsumerService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "consumer-events", groupId = "energy-tracker-group")
	public void processConsumerEvent(String message) throws JsonProcessingException {
		ConsumerEvent consumerEvent = objectMapper.readValue(message, ConsumerEvent.class);
		if (consumerEvent.isCommercial()) {
			if (consumerEvent.isActive()) {
				CommercialConsumer commercialConsumer = new CommercialConsumer();
				commercialConsumer.setDeviceId(consumerEvent.getDeviceId());
				commercialConsumer.setOwnerId(consumerEvent.getOwnerId());
				commercialConsumer.setPowerConsumption(consumerEvent.getPowerConsumption());
				commercialConsumer.setStartTime(consumerEvent.getTimestamp());
				commercialConsumerService.add(commercialConsumer);
			} else {
				CommercialConsumer commercialConsumer = commercialConsumerService.getOpenDeviceByDeviceId(consumerEvent.getDeviceId());
				commercialConsumer.setEndTime(consumerEvent.getTimestamp());
				commercialConsumerService.update(commercialConsumer);
			}
		} else {
			if (consumerEvent.isActive()) {
				Consumer consumer = new Consumer();
				consumer.setDeviceId(consumerEvent.getDeviceId());
				consumer.setOwnerId(consumerEvent.getOwnerId());
				consumer.setPowerConsumption(consumerEvent.getPowerConsumption());
				consumer.setStartTime(consumerEvent.getTimestamp());
				consumerService.add(consumer);
			} else {
				Consumer consumer = consumerService.getOpenDeviceByDeviceId(consumerEvent.getDeviceId());
				consumer.setEndTime(consumerEvent.getTimestamp());
				consumerService.update(consumer);
			}
		}
	}
}
