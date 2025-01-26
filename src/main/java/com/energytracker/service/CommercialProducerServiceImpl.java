package com.energytracker.service;

import com.energytracker.entity.CommercialProducer;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.CommercialProducerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class CommercialProducerServiceImpl implements CommercialProducerService {

	private final CommercialProducerRepository commercialProducerRepository;

	@Autowired
	public CommercialProducerServiceImpl(CommercialProducerRepository commercialProducerRepository) {
		this.commercialProducerRepository = commercialProducerRepository;
	}

	@Override
	public CommercialProducer getOpenDeviceByDeviceId(Long deviceId) {
		List<CommercialProducer> deviceList = commercialProducerRepository.findDevicesByDeviceId(deviceId);
		if (!deviceList.isEmpty()) {
			for (CommercialProducer commercialProducer : deviceList) {
				if (commercialProducer.getEndTime() == null) {
					return commercialProducer;
				}
			}
		}
		return null;
	}

	@Override
	public void update(CommercialProducer commercialProducer) {
		commercialProducerRepository.save(commercialProducer);
	}

	@Override
	@Transactional
	public void add(CommercialProducer commercialProducer) {
		CommercialProducer existingProducer = getOpenDeviceByDeviceId(commercialProducer.getDeviceId());
		if (existingProducer != null) {
			throw new DuplicateDeviceFoundException("Device with ID " + commercialProducer.getDeviceId() + " already exists");
		}
		commercialProducerRepository.save(commercialProducer);
	}

	@Override
	@Transactional
	public void remove(Long deviceId) {
		commercialProducerRepository.deleteByDeviceId(deviceId);
	}
}
