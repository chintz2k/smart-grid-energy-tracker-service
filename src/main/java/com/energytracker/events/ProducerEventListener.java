package com.energytracker.events;

import com.energytracker.entity.CommercialProducer;
import com.energytracker.entity.Producer;
import com.energytracker.service.CommercialProducerService;
import com.energytracker.service.ProducerService;
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

	private final ProducerService producerService;
	private final CommercialProducerService commercialProducerService;
	private final ObjectMapper objectMapper;

	@Autowired
	public ProducerEventListener(ProducerService producerService, CommercialProducerService commercialProducerService, ObjectMapper objectMapper) {
		this.producerService = producerService;
		this.commercialProducerService = commercialProducerService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "producer-events", groupId = "energy-tracker-group")
	public void processProducerEvent(String message) throws JsonProcessingException {
		ProducerEvent producerEvent = objectMapper.readValue(message, ProducerEvent.class);
		if (producerEvent.isCommercial()) {
			if (producerEvent.isActive()) {
				CommercialProducer commercialProducer = new CommercialProducer();
				commercialProducer.setDeviceId(producerEvent.getDeviceId());
				commercialProducer.setOwnerId(producerEvent.getOwnerId());
				commercialProducer.setPowerProduction(producerEvent.getPowerProduction());
				commercialProducer.setStartTime(producerEvent.getTimestamp());
				commercialProducerService.add(commercialProducer);
			} else {
				CommercialProducer commercialProducer = commercialProducerService.getOpenDeviceByDeviceId(producerEvent.getDeviceId());
				commercialProducer.setEndTime(producerEvent.getTimestamp());
				commercialProducerService.update(commercialProducer);
			}
		} else {
			if (producerEvent.isActive()) {
				Producer producer = new Producer();
				producer.setDeviceId(producerEvent.getDeviceId());
				producer.setOwnerId(producerEvent.getOwnerId());
				producer.setPowerProduction(producerEvent.getPowerProduction());
				producer.setStartTime(producerEvent.getTimestamp());
				producerService.add(producer);
			} else {
				Producer producer = producerService.getOpenDeviceByDeviceId(producerEvent.getDeviceId());
				producer.setEndTime(producerEvent.getTimestamp());
				producerService.update(producer);
			}
		}
	}
}
