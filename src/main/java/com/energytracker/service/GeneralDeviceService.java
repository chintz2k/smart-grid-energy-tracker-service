package com.energytracker.service;

import com.energytracker.entity.BaseDevice;
import com.energytracker.exception.DuplicateDeviceFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * @author André Heinen
 */
public interface GeneralDeviceService<T extends BaseDevice> {

	@Transactional
	void add(T device) throws DuplicateDeviceFoundException;

	@Transactional(readOnly = true)
	List<T> getAll();

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
