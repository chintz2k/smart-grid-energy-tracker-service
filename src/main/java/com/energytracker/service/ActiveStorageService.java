package com.energytracker.service;

import com.energytracker.entity.ActiveStorage;

import java.util.Optional;

/**
 * @author André Heinen
 */
public interface ActiveStorageService {

	Optional<ActiveStorage> findByDeviceId(Long deviceId);

	void addCommercial(ActiveStorage activeStorage);
	void removeCommercial(Long deviceId);

	void addPrivate(ActiveStorage activeStorage);
	void removePrivate(Long deviceId);

}
