package com.energytracker.events;

import com.energytracker.entity.ActiveProducer;
import com.energytracker.service.ActiveProducerService;
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

	private final ActiveProducerService activeProducerService;

	@Autowired
	public ProducerEventListener(ActiveProducerService activeProducerService) {
		this.activeProducerService = activeProducerService;
	}

	@KafkaListener(topics = "producer-events", groupId = "energy-tracker-group")
	public void processProducerEvent(String message) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		ProducerEvent producerEvent = objectMapper.readValue(message, ProducerEvent.class);

		ActiveProducer activeProducer = new ActiveProducer();
		activeProducer.setDeviceId(producerEvent.getDeviceId());
		activeProducer.setOwnerId(producerEvent.getOwnerId());
		activeProducer.setPowerProduction(producerEvent.getPowerProduction());
		activeProducer.setTimestamp(producerEvent.getTimestamp());

		boolean commercial = producerEvent.isCommercial();

		if (producerEvent.isActive()) {
			if (commercial) {
				activeProducerService.addCommercial(activeProducer);
			} else {
				activeProducerService.addPrivate(activeProducer);
			}
		} else {
			if (commercial) {
				activeProducerService.removeCommercial(producerEvent.getDeviceId());
			} else {
				activeProducerService.removePrivate(producerEvent.getDeviceId());
			}
		}
	}
}
