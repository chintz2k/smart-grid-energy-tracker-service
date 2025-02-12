package com.energytracker.service;

import com.energytracker.entity.BaseDevice;
import com.energytracker.exception.DuplicateDeviceFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * @author André Heinen
 */
public abstract class GeneralDeviceServiceImpl<T extends BaseDevice, R extends JpaRepository<T, Long>> implements GeneralDeviceService<T> {

	protected final R repository;

	protected GeneralDeviceServiceImpl(R repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void add(T device) throws DuplicateDeviceFoundException {
		T existing = getOpenDeviceByDeviceId(device.getDeviceId());
		if (existing != null) {
			throw new DuplicateDeviceFoundException("Duplicate device found! ID = " + device.getDeviceId());
		}
		repository.save(device);
	}

	@Override
	@Cacheable("devices")
	@Transactional(readOnly = true)
	public List<T> getAll() {
		return repository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public T getDeviceById(Long id) {
		return repository.findById(id).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public abstract T getOpenDeviceByDeviceId(Long deviceId);

	@Override
	@Transactional
	public void remove(T device) {
		repository.delete(device);
	}

	@Override
	@Transactional
	public void removeAll(List<T> devices) {
		repository.deleteAll(devices);
	}

	@Override
	@Transactional
	public void update(T device) {
		repository.save(device);
	}

	@Override
	@Transactional
	public void updateAll(List<T> devices) {
		repository.saveAll(devices);
	}

	@Override
	@Transactional
	public void updateEndTime(Long deviceId, Instant endTime) {
		T device = getOpenDeviceByDeviceId(deviceId);
		if (device != null) {
			device.setEndTime(endTime);
			repository.save(device);
		}
	}
}
