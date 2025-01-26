package com.energytracker.service;

import com.energytracker.entity.Storage;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.StorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class StorageServiceImpl implements StorageService {

	private final StorageRepository storageRepository;

	@Autowired
	public StorageServiceImpl(StorageRepository storageRepository) {
		this.storageRepository = storageRepository;
	}

	@Override
	public Storage getOpenDeviceByDeviceId(Long deviceId) {
		List<Storage> deviceList = storageRepository.findDevicesByDeviceId(deviceId);
		if (!deviceList.isEmpty()) {
			for (Storage storage : deviceList) {
				if (storage.getEndTime() == null) {
					return storage;
				}
			}
		}
		return null;
	}

	@Override
	public void update(Storage storage) {
		storageRepository.save(storage);
	}

	@Override
	@Transactional
	public void add(Storage storage) {
		Storage existingStorage = getOpenDeviceByDeviceId(storage.getDeviceId());
		if (existingStorage != null) {
			throw new DuplicateDeviceFoundException("Device with ID " + storage.getDeviceId() + " already exists");
		}
		storageRepository.save(storage);
	}

	@Override
	@Transactional
	public void remove(Long deviceId) {
		storageRepository.deleteByDeviceId(deviceId);
	}
}
