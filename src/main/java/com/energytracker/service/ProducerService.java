package com.energytracker.service;

import com.energytracker.entity.Producer;

/**
 * @author André Heinen
 */
public interface ProducerService {

	Producer getOpenDeviceByDeviceId(Long deviceId);

	void update(Producer producer);

	void add(Producer producer);
	void remove(Long deviceId);

}
