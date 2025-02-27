package com.energytracker.service.general;

import com.energytracker.entity.devices.bases.BaseDevice;
import com.energytracker.exception.exceptions.DuplicateDeviceFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * @author André Heinen
 */
public interface GeneralDeviceService<T extends BaseDevice> {

	@Transactional
	void add(T device) throws DuplicateDeviceFoundException;

	@Transactional(readOnly = true)
	List<T> getAll();

	@Transactional(readOnly = true)
	Set<Long> getAllDeviceIds();

	@Transactional(readOnly = true)
	T getDeviceById(Long id);

	@Transactional(readOnly = true)
	T getOpenDeviceByDeviceId(Long deviceId);

	@Transactional
	void remove(T device);

	@Transactional
	void removeAll(List<T> devices);

	@Transactional
	void update(T device);

	@Transactional
	void updateAll(List<T> devices);

	@Transactional
	void updateEndTime(Long id, Instant endTime);

}
