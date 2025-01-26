package com.energytracker.service;

import com.energytracker.entity.Consumer;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.ConsumerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class ConsumerServiceImpl implements ConsumerService {

	private final ConsumerRepository consumerRepository;

	@Autowired
	public ConsumerServiceImpl(ConsumerRepository consumerRepository) {
		this.consumerRepository = consumerRepository;
	}

	@Override
	public Consumer getOpenDeviceByDeviceId(Long deviceId) {
		List<Consumer> deviceList = consumerRepository.findDevicesByDeviceId(deviceId);
		if (!deviceList.isEmpty()) {
			for (Consumer consumer : deviceList) {
				if (consumer.getEndTime() == null) {
					return consumer;
				}
			}
		}
		return null;
	}

	@Override
	public void update(Consumer consumer) {
		consumerRepository.save(consumer);
	}

	@Override
	@Transactional
	public void add(Consumer consumer) {
		Consumer existingConsumer = getOpenDeviceByDeviceId(consumer.getDeviceId());
		if (existingConsumer != null) {
			throw new DuplicateDeviceFoundException("Device with ID " + consumer.getDeviceId() + " already exists");
		}
		consumerRepository.save(consumer);
	}

	@Override
	@Transactional
	public void remove(Long deviceId) {
		consumerRepository.deleteByDeviceId(deviceId);
	}
}
