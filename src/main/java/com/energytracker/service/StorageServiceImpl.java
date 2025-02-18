package com.energytracker.service;

import com.energytracker.entity.Storage;
import com.energytracker.repository.StorageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * @author André Heinen
 */
@Service
public class StorageServiceImpl extends GeneralDeviceServiceImpl<Storage, StorageRepository> {

	public StorageServiceImpl(StorageRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	public Storage getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}

	@Override
	@Transactional
	public void updateEndTime(Long deviceId, Instant endTime) {
		Storage device = getOpenDeviceByDeviceId(deviceId);
		if (device != null) {
			repository.delete(device);
		}
	}
}
