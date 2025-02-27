package com.energytracker.kafka.listener;

import com.energytracker.entity.devices.CommercialProducer;
import com.energytracker.entity.devices.Producer;
import com.energytracker.kafka.events.ProducerEvent;
import com.energytracker.service.general.GeneralDeviceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * @author André Heinen
 */
@Service
public class ProducerEventListener {

	private final GeneralDeviceService<Producer> producerService;
	private final GeneralDeviceService<CommercialProducer> commercialProducerService;

	private final ObjectMapper objectMapper;

	@Autowired
	public ProducerEventListener(GeneralDeviceService<Producer> producerService, GeneralDeviceService<CommercialProducer> commercialProducerService, ObjectMapper objectMapper) {
		this.producerService = producerService;
		this.commercialProducerService = commercialProducerService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "producer-events", groupId = "energy-tracker-group")
	public void processProducerEvent(String message) throws JsonProcessingException {
		ProducerEvent producerEvent = objectMapper.readValue(message, ProducerEvent.class);
		if (producerEvent.isCommercial()) {
			if (producerEvent.isActive()) {
				CommercialProducer commercialProducer = producerEvent.toCommercialProducer();
				commercialProducerService.add(commercialProducer);
			} else {
				commercialProducerService.updateEndTime(producerEvent.getDeviceId(), producerEvent.getTimestamp());
			}
		} else {
			if (producerEvent.isActive()) {
				Producer producer = producerEvent.toProducer();
				producerService.add(producer);
			} else {
				producerService.updateEndTime(producerEvent.getDeviceId(), producerEvent.getTimestamp());
			}
		}
	}
}
