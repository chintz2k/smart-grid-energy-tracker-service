package com.energytracker.service;

import com.energytracker.entity.Consumer;

/**
 * @author André Heinen
 */
public interface ConsumerService {

	Consumer getOpenDeviceByDeviceId(Long deviceId);

	void update(Consumer consumer);

	void add(Consumer consumer);
	void remove(Long deviceId);

}
