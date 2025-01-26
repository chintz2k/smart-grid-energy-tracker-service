package com.energytracker.service;

import com.energytracker.entity.CommercialConsumer;
import com.energytracker.exception.DuplicateDeviceFoundException;
import com.energytracker.repository.CommercialConsumerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class CommercialConsumerServiceImpl implements CommercialConsumerService {

	private final CommercialConsumerRepository commercialConsumerRepository;

	@Autowired
	public CommercialConsumerServiceImpl(CommercialConsumerRepository commercialConsumerRepository) {
		this.commercialConsumerRepository = commercialConsumerRepository;
	}

	@Override
	public CommercialConsumer getOpenDeviceByDeviceId(Long deviceId) {
		List<CommercialConsumer> deviceList = commercialConsumerRepository.findDevicesByDeviceId(deviceId);
		if (!deviceList.isEmpty()) {
			for (CommercialConsumer commercialConsumer : deviceList) {
				if (commercialConsumer.getEndTime() == null) {
					return commercialConsumer;
				}
			}
		}
		return null;
	}

	@Override
	public void update(CommercialConsumer commercialConsumer) {
		commercialConsumerRepository.save(commercialConsumer);
	}

	@Override
	@Transactional
	public void add(CommercialConsumer commercialConsumer) {
		CommercialConsumer existingConsumer = getOpenDeviceByDeviceId(commercialConsumer.getDeviceId());
		if (existingConsumer != null) {
			throw new DuplicateDeviceFoundException("Device with ID " + commercialConsumer.getDeviceId() + " already exists");
		}
		commercialConsumerRepository.save(commercialConsumer);
	}

	@Override
	@Transactional
	public void remove(Long deviceId) {
		commercialConsumerRepository.deleteByDeviceId(deviceId);
	}
}
