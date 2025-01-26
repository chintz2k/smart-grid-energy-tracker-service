package com.energytracker.events;

import com.energytracker.entity.CommercialStorage;
import com.energytracker.entity.Storage;
import com.energytracker.service.CommercialStorageService;
import com.energytracker.service.StorageService;
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

	private final StorageService storageService;
	private final CommercialStorageService commercialStorageService;
	private final ObjectMapper objectMapper;

	@Autowired
	public StorageEventListener(StorageService storageService, CommercialStorageService commercialStorageService, ObjectMapper objectMapper) {
		this.storageService = storageService;
		this.commercialStorageService = commercialStorageService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "storage-events", groupId = "energy-tracker-group")
	public void processStorageEvent(String message) throws JsonProcessingException {
		StorageEvent storageEvent = objectMapper.readValue(message, StorageEvent.class);
		if (storageEvent.isCommercial()) {
			if (storageEvent.isActive()) {
				CommercialStorage commercialStorage = new CommercialStorage();
				commercialStorage.setDeviceId(storageEvent.getDeviceId());
				commercialStorage.setOwnerId(storageEvent.getOwnerId());
				commercialStorage.setCapacity(storageEvent.getCapacity());
				commercialStorage.setCurrentCharge(storageEvent.getCurrentCharge());
				commercialStorage.setChargingPriority(storageEvent.getChargingPriority());
				commercialStorage.setConsumingPriority(storageEvent.getConsumingPriority());
				commercialStorage.setStartTime(storageEvent.getTimestamp());
				commercialStorageService.add(commercialStorage);
			} else {
				CommercialStorage commercialStorage = commercialStorageService.getOpenDeviceByDeviceId(storageEvent.getDeviceId());
				commercialStorage.setEndTime(storageEvent.getTimestamp());
				commercialStorageService.update(commercialStorage);
			}
		} else {
			if (storageEvent.isActive()) {
				Storage storage = new Storage();
				storage.setDeviceId(storageEvent.getDeviceId());
				storage.setOwnerId(storageEvent.getOwnerId());
				storage.setCapacity(storageEvent.getCapacity());
				storage.setCurrentCharge(storageEvent.getCurrentCharge());
				storage.setChargingPriority(storageEvent.getChargingPriority());
				storage.setConsumingPriority(storageEvent.getConsumingPriority());
				storage.setStartTime(storageEvent.getTimestamp());
				storageService.add(storage);
			} else {
				Storage storage = storageService.getOpenDeviceByDeviceId(storageEvent.getDeviceId());
				storage.setEndTime(storageEvent.getTimestamp());
				storageService.update(storage);
			}
		}
	}
}
