package com.energytracker.service;

import com.energytracker.entity.Producer;
import com.energytracker.repository.ProducerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author André Heinen
 */
@Service
public class ProducerServiceImpl extends GeneralDeviceServiceImpl<Producer, ProducerRepository> {

	public ProducerServiceImpl(ProducerRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	public Producer getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
