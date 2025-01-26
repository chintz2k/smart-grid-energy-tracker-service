package com.energytracker.service;

import com.energytracker.entity.SmartConsumer;

/**
 * @author André Heinen
 */
public interface SmartConsumerService {

	SmartConsumer getOpenDeviceByDeviceId(Long deviceId);

	void update(SmartConsumer smartConsumer);

	void add(SmartConsumer smartConsumer);
	void remove(Long deviceId);

}
