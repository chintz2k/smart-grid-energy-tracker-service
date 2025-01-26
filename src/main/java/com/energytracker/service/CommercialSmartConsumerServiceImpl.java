package com.energytracker.service;

import com.energytracker.entity.CommercialSmartConsumer;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.CommercialSmartConsumerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class CommercialSmartConsumerServiceImpl implements CommercialSmartConsumerService {

	private final CommercialSmartConsumerRepository commercialSmartConsumerRepository;

	@Autowired
	public CommercialSmartConsumerServiceImpl(CommercialSmartConsumerRepository commercialSmartConsumerRepository) {
		this.commercialSmartConsumerRepository = commercialSmartConsumerRepository;
	}

	@Override
	public CommercialSmartConsumer getOpenDeviceByDeviceId(Long deviceId) {
		List<CommercialSmartConsumer> deviceList = commercialSmartConsumerRepository.findDevicesByDeviceId(deviceId);
		if (!deviceList.isEmpty()) {
			for (CommercialSmartConsumer commercialSmartConsumer : deviceList) {
				if (commercialSmartConsumer.getEndTime() == null) {
					return commercialSmartConsumer;
				}
			}
		}
		return null;
	}

	@Override
	public void update(CommercialSmartConsumer commercialSmartConsumer) {
		commercialSmartConsumerRepository.save(commercialSmartConsumer);
	}

	@Override
	@Transactional
	public void add(CommercialSmartConsumer commercialSmartConsumer) {
		CommercialSmartConsumer existingSmartConsumer = getOpenDeviceByDeviceId(commercialSmartConsumer.getDeviceId());
		if (existingSmartConsumer != null) {
			throw new DuplicateDeviceFoundException("Device with ID " + commercialSmartConsumer.getDeviceId() + " already exists");
		}
		commercialSmartConsumerRepository.save(commercialSmartConsumer);
	}

	@Override
	@Transactional
	public void remove(Long deviceId) {
		commercialSmartConsumerRepository.deleteByDeviceId(deviceId);
	}
}
