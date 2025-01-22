package com.energytracker.service;

import com.energytracker.entity.ActiveStorage;
import com.energytracker.repository.ActiveCommercialStorageRepository;
import com.energytracker.repository.ActiveStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author André Heinen
 */
@Service
public class ActiveStorageServiceImpl implements ActiveStorageService {

	private final ActiveStorageRepository activeStorageRepository;
	private final ActiveCommercialStorageRepository activeCommercialStorageRepository;

	@Autowired
	public ActiveStorageServiceImpl(ActiveStorageRepository activeStorageRepository, ActiveCommercialStorageRepository activeCommercialStorageRepository) {
		this.activeStorageRepository = activeStorageRepository;
		this.activeCommercialStorageRepository = activeCommercialStorageRepository;
	}

	@Override
	public Optional<ActiveStorage> findByDeviceId(Long deviceId) {
		return activeStorageRepository.findByDeviceId(deviceId);
	}

	@Override
	public void addCommercial(ActiveStorage activeStorage) {
		activeCommercialStorageRepository.save(activeStorage);
	}

	@Override
	public void removeCommercial(Long deviceId) {
		activeCommercialStorageRepository.deleteByDeviceId(deviceId);
	}

	@Override
	public void addPrivate(ActiveStorage activeStorage) {
		activeStorageRepository.save(activeStorage);
	}

	@Override
	public void removePrivate(Long deviceId) {
		activeStorageRepository.deleteByDeviceId(deviceId);
	}
}
