package com.energytracker.service;

import com.energytracker.entity.CommercialSmartConsumer;
import com.energytracker.repository.CommercialSmartConsumerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author André Heinen
 */
@Service
public class CommercialSmartConsumerServiceImpl extends GeneralDeviceServiceImpl<CommercialSmartConsumer, CommercialSmartConsumerRepository> {

	public CommercialSmartConsumerServiceImpl(CommercialSmartConsumerRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	public CommercialSmartConsumer getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
