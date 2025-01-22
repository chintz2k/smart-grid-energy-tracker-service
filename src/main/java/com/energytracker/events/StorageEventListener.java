package com.energytracker.events;

import com.energytracker.entity.ActiveStorage;
import com.energytracker.service.ActiveStorageService;
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

	private final ActiveStorageService activeStorageService;
	private final ObjectMapper objectMapper;

	@Autowired
	public StorageEventListener(ActiveStorageService activeStorageService, ObjectMapper objectMapper) {
		this.activeStorageService = activeStorageService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "storage-events", groupId = "energy-tracker-group")
	public void processStorageEvent(String message) throws JsonProcessingException {
		StorageEvent storageEvent = objectMapper.readValue(message, StorageEvent.class);

		ActiveStorage activeStorage = new ActiveStorage();
		activeStorage.setDeviceId(storageEvent.getDeviceId());
		activeStorage.setOwnerId(storageEvent.getOwnerId());
		activeStorage.setCapacity(storageEvent.getCapacity());
		activeStorage.setCurrentCharge(storageEvent.getCurrentCharge());
		activeStorage.setChargingPriority(storageEvent.getChargingPriority());
		activeStorage.setConsumingPriority(storageEvent.getConsumingPriority());
		activeStorage.setTimestamp(storageEvent.getTimestamp());

		boolean commercial = storageEvent.isCommercial();

		if (storageEvent.isActive()) {
			if (commercial) {
				activeStorageService.addCommercial(activeStorage);
			} else {
				activeStorageService.addPrivate(activeStorage);
			}
		} else {
			if (commercial) {
				activeStorageService.removeCommercial(storageEvent.getDeviceId());
			} else {
				activeStorageService.removePrivate(storageEvent.getDeviceId());
			}
		}
	}
}
