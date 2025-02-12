package com.energytracker.service;

import com.energytracker.entity.SmartConsumer;
import com.energytracker.repository.SmartConsumerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author André Heinen
 */
@Service
public class SmartConsumerServiceImpl extends GeneralDeviceServiceImpl<SmartConsumer, SmartConsumerRepository> {

	public SmartConsumerServiceImpl(SmartConsumerRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	public SmartConsumer getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
