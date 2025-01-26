package com.energytracker.service;

import com.energytracker.entity.SmartConsumer;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.SmartConsumerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class SmartConsumerServiceImpl implements SmartConsumerService {

	private final SmartConsumerRepository smartConsumerRepository;

	@Autowired
	public SmartConsumerServiceImpl(SmartConsumerRepository smartConsumerRepository) {
		this.smartConsumerRepository = smartConsumerRepository;
	}

	@Override
	public SmartConsumer getOpenDeviceByDeviceId(Long deviceId) {
		List<SmartConsumer> deviceList = smartConsumerRepository.findDevicesByDeviceId(deviceId);
		if (!deviceList.isEmpty()) {
			for (SmartConsumer smartConsumer : deviceList) {
				if (smartConsumer.getEndTime() == null) {
					return smartConsumer;
				}
			}
		}
		return null;
	}

	@Override
	public void update(SmartConsumer smartConsumer) {
		smartConsumerRepository.save(smartConsumer);
	}

	@Override
	@Transactional
	public void add(SmartConsumer smartConsumer) {
		SmartConsumer existingConsumer = getOpenDeviceByDeviceId(smartConsumer.getDeviceId());
		if (existingConsumer != null) {
			throw new DuplicateDeviceFoundException("Device with ID " + smartConsumer.getDeviceId() + " already exists");
		}
		smartConsumerRepository.save(smartConsumer);
	}

	@Override
	@Transactional
	public void remove(Long deviceId) {
		smartConsumerRepository.deleteByDeviceId(deviceId);
	}
}
