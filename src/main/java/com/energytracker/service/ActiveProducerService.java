package com.energytracker.service;

import com.energytracker.entity.ActiveProducer;

import java.util.Optional;

/**
 * @author André Heinen
 */
public interface ActiveProducerService {

	Optional<ActiveProducer> findByDeviceId(Long deviceId);

	void addCommercial(ActiveProducer activeProducer);
	void removeCommercial(Long deviceId);

	void addPrivate(ActiveProducer activeProducer);
	void removePrivate(Long deviceId);

}
