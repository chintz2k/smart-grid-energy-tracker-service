package com.energytracker.service;

import com.energytracker.entity.ActiveConsumer;

import java.util.Optional;

/**
 * @author André Heinen
 */
public interface ActiveConsumerService {

	Optional<ActiveConsumer> getByDeviceId(Long deviceId);

	void addCommercial(ActiveConsumer activeConsumer);
	void removeCommercial(Long deviceId);

	void addPrivate(ActiveConsumer activeConsumer);
	void removePrivate(Long deviceId);

}
