package com.energytracker.service;

import com.energytracker.entity.Consumer;
import com.energytracker.repository.ConsumerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author André Heinen
 */
@Service
public class ConsumerServiceImpl extends GeneralDeviceServiceImpl<Consumer, ConsumerRepository> {

	public ConsumerServiceImpl(ConsumerRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	public Consumer getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
