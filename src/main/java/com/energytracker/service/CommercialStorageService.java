package com.energytracker.service;

import com.energytracker.entity.CommercialStorage;

/**
 * @author André Heinen
 */
public interface CommercialStorageService {

	CommercialStorage getOpenDeviceByDeviceId(Long deviceId);

	void update(CommercialStorage commercialStorage);

	void add(CommercialStorage commercialStorage);
	void remove(Long deviceId);

}
