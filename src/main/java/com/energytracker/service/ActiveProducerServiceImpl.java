package com.energytracker.service;

import com.energytracker.entity.ActiveProducer;
import com.energytracker.repository.ActiveCommercialProducerRepository;
import com.energytracker.repository.ActiveProducerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author André Heinen
 */
@Service
public class ActiveProducerServiceImpl implements ActiveProducerService {

	private final ActiveProducerRepository activeProducerRepository;
	private final ActiveCommercialProducerRepository activeCommercialProducerRepository;

	@Autowired
	public ActiveProducerServiceImpl(ActiveProducerRepository activeProducerRepository, ActiveCommercialProducerRepository activeCommercialProducerRepository) {
		this.activeProducerRepository = activeProducerRepository;
		this.activeCommercialProducerRepository = activeCommercialProducerRepository;
	}

	@Override
	public Optional<ActiveProducer> findByDeviceId(Long deviceId) {
		return activeProducerRepository.findByDeviceId(deviceId);
	}

	@Override
	public void addCommercial(ActiveProducer activeProducer) {
		activeCommercialProducerRepository.save(activeProducer);
	}

	@Override
	public void removeCommercial(Long deviceId) {
		activeCommercialProducerRepository.deleteByDeviceId(deviceId);
	}

	@Override
	public void addPrivate(ActiveProducer activeProducer) {
		activeProducerRepository.save(activeProducer);
	}

	@Override
	public void removePrivate(Long deviceId) {
		activeProducerRepository.deleteByDeviceId(deviceId);
	}
}
