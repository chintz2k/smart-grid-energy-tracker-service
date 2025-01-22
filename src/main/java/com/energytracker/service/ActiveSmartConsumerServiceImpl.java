package com.energytracker.service;

import com.energytracker.entity.ActiveSmartConsumer;
import com.energytracker.repository.ActiveCommercialSmartConsumerRepository;
import com.energytracker.repository.ActiveSmartConsumerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author André Heinen
 */
@Service
public class ActiveSmartConsumerServiceImpl implements ActiveSmartConsumerService {

	private final ActiveSmartConsumerRepository activeSmartConsumerRepository;
	private final ActiveCommercialSmartConsumerRepository activeCommercialSmartConsumerRepository;

	@Autowired
	public ActiveSmartConsumerServiceImpl(ActiveSmartConsumerRepository activeSmartConsumerRepository, ActiveCommercialSmartConsumerRepository activeCommercialSmartConsumerRepository) {
		this.activeSmartConsumerRepository = activeSmartConsumerRepository;
		this.activeCommercialSmartConsumerRepository = activeCommercialSmartConsumerRepository;
	}

	@Override
	public Optional<ActiveSmartConsumer> findByDeviceId(Long deviceId) {
		return activeSmartConsumerRepository.findByDeviceId(deviceId);
	}

	@Override
	public void addCommercial(ActiveSmartConsumer activeSmartConsumer) {
		activeCommercialSmartConsumerRepository.save(activeSmartConsumer);
	}

	@Override
	public void removeCommercial(Long deviceId) {
		activeCommercialSmartConsumerRepository.deleteByDeviceId(deviceId);
	}

	@Override
	public void addPrivate(ActiveSmartConsumer activeSmartConsumer) {
		activeSmartConsumerRepository.save(activeSmartConsumer);
	}

	@Override
	public void removePrivate(Long deviceId) {
		activeSmartConsumerRepository.deleteByDeviceId(deviceId);
	}
}
