package com.energytracker.service;

import com.energytracker.entity.Storage;

/**
 * @author André Heinen
 */
public interface StorageService {

	Storage getOpenDeviceByDeviceId(Long deviceId);

	void update(Storage storage);

	void add(Storage storage);
	void remove(Long deviceId);

}
