package com.energytracker.service;

import com.energytracker.entity.ActiveSmartConsumer;

import java.util.Optional;

/**
 * @author André Heinen
 */
public interface ActiveSmartConsumerService {

	Optional<ActiveSmartConsumer> findByDeviceId(Long deviceId);

	void addCommercial(ActiveSmartConsumer activeSmartConsumer);
	void removeCommercial(Long deviceId);

	void addPrivate(ActiveSmartConsumer activeSmartConsumer);
	void removePrivate(Long deviceId);

}
