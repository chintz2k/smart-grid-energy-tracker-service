package com.energytracker.service;

import com.energytracker.entity.CommercialConsumer;
import com.energytracker.repository.CommercialConsumerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author André Heinen
 */
@Service
public class CommercialConsumerServiceImpl extends GeneralDeviceServiceImpl<CommercialConsumer, CommercialConsumerRepository> {

	protected CommercialConsumerServiceImpl(CommercialConsumerRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	public CommercialConsumer getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
