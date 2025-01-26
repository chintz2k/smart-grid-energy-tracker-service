package com.energytracker.service;

import com.energytracker.entity.Producer;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.ProducerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class ProducerServiceImpl implements ProducerService {

	private final ProducerRepository producerRepository;

	@Autowired
	public ProducerServiceImpl(ProducerRepository producerRepository) {
		this.producerRepository = producerRepository;
	}

	@Override
	public Producer getOpenDeviceByDeviceId(Long deviceId) {
		List<Producer> deviceList = producerRepository.findDevicesByDeviceId(deviceId);
		if (!deviceList.isEmpty()) {
			for (Producer producer : deviceList) {
				if (producer.getEndTime() == null) {
					return producer;
				}
			}
		}
		return null;
	}

	@Override
	public void update(Producer producer) {
		producerRepository.save(producer);
	}

	@Override
	@Transactional
	public void add(Producer producer) {
		Producer existingProducer = getOpenDeviceByDeviceId(producer.getDeviceId());
		if (existingProducer != null) {
			throw new DuplicateDeviceFoundException("Device with ID " + producer.getDeviceId() + " already exists");
		}
		producerRepository.save(producer);
	}

	@Override
	@Transactional
	public void remove(Long deviceId) {
		producerRepository.deleteByDeviceId(deviceId);
	}
}
