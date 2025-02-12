package com.energytracker.service;

import com.energytracker.entity.Storage;
import com.energytracker.repository.StorageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
