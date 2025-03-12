package com.energytracker.kafka.listener;

import com.energytracker.entity.devices.CommercialStorage;
import com.energytracker.entity.devices.Storage;
import com.energytracker.kafka.events.StorageEvent;
import com.energytracker.kafka.events.StorageSystemEvent;
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
public class StorageEventListener {

	private static final Logger logger = LoggerFactory.getLogger(StorageEventListener.class);

	private final GeneralDeviceService<Storage> storageService;
	private final GeneralDeviceService<CommercialStorage> commercialStorageService;

	private final ObjectMapper objectMapper;

	@Autowired
	public StorageEventListener(GeneralDeviceService<Storage> storageService, GeneralDeviceService<CommercialStorage> commercialStorageService, ObjectMapper objectMapper) {
		this.storageService = storageService;
		this.commercialStorageService = commercialStorageService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "storage-events", groupId = "energy-tracker-group")
	public void processStorageEvent(String message) throws JsonProcessingException {
		StorageEvent storageEvent = objectMapper.readValue(message, StorageEvent.class);
		if (storageEvent.isCommercial()) {
			if (storageEvent.isActive()) {
				CommercialStorage commercialStorage = storageEvent.toCommercialStorage();
				commercialStorageService.add(commercialStorage);
			} else {
				commercialStorageService.updateEndTime(storageEvent.getDeviceId(), storageEvent.getTimestamp());
			}
		} else {
			if (storageEvent.isActive()) {
				Storage storage = storageEvent.toStorage();
				storageService.add(storage);
			} else {
				storageService.updateEndTime(storageEvent.getDeviceId(), storageEvent.getTimestamp());
			}
		}
	}

	@KafkaListener(topics = "system-managed-private-storage-events", groupId = "energy-tracker-group")
	public void processStorageEventsFromSystem(@Payload String message) throws JsonProcessingException {
		try {
			List<StorageSystemEvent> events = objectMapper.readValue(message, new TypeReference<>() {});

			for (StorageSystemEvent event : events) {
				Storage storage = new Storage();
				storage.setDeviceId(event.getDeviceId());
				storage.setOwnerId(event.getOwnerId());
				storage.setCapacity(event.getCapacity());
				storage.setChargingPriority(event.getChargingPriority());
				storage.setConsumingPriority(event.getConsumingPriority());
				storage.setCurrentCharge(event.getCurrentCharge());
				storage.setStartTime(event.getEventStart());
				storage.setEndTime(event.getEventEnd());

				storageService.systemSave(storage);
			}
		} catch (JsonProcessingException e) {
			logger.error("Error processing Kafka message: {}", message, e);
			throw e;
		}
	}
}
