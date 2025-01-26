package com.energytracker.service;

import com.energytracker.entity.CommercialStorage;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.CommercialStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class CommercialStorageServiceImpl implements CommercialStorageService {

	private final CommercialStorageRepository commercialStorageRepository;

	@Autowired
	public CommercialStorageServiceImpl(CommercialStorageRepository commercialStorageRepository) {
		this.commercialStorageRepository = commercialStorageRepository;
	}

	@Override
	public CommercialStorage getOpenDeviceByDeviceId(Long deviceId) {
		List<CommercialStorage> deviceList = commercialStorageRepository.findDevicesByDeviceId(deviceId);
		if (!deviceList.isEmpty()) {
			for (CommercialStorage commercialStorage : deviceList) {
				if (commercialStorage.getEndTime() == null) {
					return commercialStorage;
				}
			}
		}
		return null;
	}

	@Override
	public void update(CommercialStorage commercialStorage) {
		commercialStorageRepository.save(commercialStorage);
	}

	@Override
	@Transactional
	public void add(CommercialStorage commercialStorage) {
		CommercialStorage existingStorage = getOpenDeviceByDeviceId(commercialStorage.getDeviceId());
		if (existingStorage != null) {
			throw new DuplicateDeviceFoundException("Device with ID " + commercialStorage.getDeviceId() + " already exists");
		}
		commercialStorageRepository.save(commercialStorage);
	}

	@Override
	@Transactional
	public void remove(Long deviceId) {
		commercialStorageRepository.deleteByDeviceId(deviceId);
	}
}
