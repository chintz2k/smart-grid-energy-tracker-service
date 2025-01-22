package com.energytracker.service;

import com.energytracker.entity.ActiveConsumer;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.ActiveCommercialConsumerRepository;
import com.energytracker.repository.ActiveConsumerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author André Heinen
 */
@Service
public class ActiveConsumerServiceImpl implements ActiveConsumerService {

	private final ActiveConsumerRepository activeConsumerRepository;
	private final ActiveCommercialConsumerRepository activeCommercialConsumerRepository;

	@Autowired
	public ActiveConsumerServiceImpl(ActiveConsumerRepository activeConsumerRepository, ActiveCommercialConsumerRepository activeCommercialConsumerRepository) {
		this.activeConsumerRepository = activeConsumerRepository;
		this.activeCommercialConsumerRepository = activeCommercialConsumerRepository;
	}

	@Override
	public Optional<ActiveConsumer> getByDeviceId(Long deviceId) {
		return activeCommercialConsumerRepository.findByDeviceId(deviceId)
				.or(() -> activeConsumerRepository.findByDeviceId(deviceId));
	}

	@Override
	public void addCommercial(ActiveConsumer activeConsumer) {
		Optional<ActiveConsumer> existingConsumer = getByDeviceId(activeConsumer.getDeviceId());
		if (existingConsumer.isPresent()) {
			throw new DuplicateDeviceFoundException("Device with ID " + activeConsumer.getDeviceId() + " already exists");
		}
		activeCommercialConsumerRepository.save(activeConsumer);
	}

	@Override
	public void removeCommercial(Long deviceId) {
		activeCommercialConsumerRepository.deleteByDeviceId(deviceId);
	}

	@Override
	public void addPrivate(ActiveConsumer activeConsumer) {
		Optional<ActiveConsumer> existingConsumer = getByDeviceId(activeConsumer.getDeviceId());
		if (existingConsumer.isPresent()) {
			throw new DuplicateDeviceFoundException("Device with ID " + activeConsumer.getDeviceId() + " already exists");
		}
		activeConsumerRepository.save(activeConsumer);
	}

	@Override
	public void removePrivate(Long deviceId) {
		activeConsumerRepository.deleteByDeviceId(deviceId);
	}
}
