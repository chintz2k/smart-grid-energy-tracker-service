package com.energytracker.service;

import com.energytracker.entity.CommercialStorage;
import com.energytracker.repository.CommercialStorageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author André Heinen
 */
@Service
public class CommercialStorageServiceImpl extends GeneralDeviceServiceImpl<CommercialStorage, CommercialStorageRepository> {

	public CommercialStorageServiceImpl(CommercialStorageRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	public CommercialStorage getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
