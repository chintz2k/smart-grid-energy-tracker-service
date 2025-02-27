package com.energytracker.kafka.listener;

import com.energytracker.entity.devices.CommercialStorage;
import com.energytracker.entity.devices.Storage;
import com.energytracker.kafka.events.StorageEvent;
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
public class StorageEventListener {

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
}
