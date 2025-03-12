package com.energytracker.service.devices;

import com.energytracker.entity.devices.CommercialConsumer;
import com.energytracker.repository.devices.CommercialConsumerRepository;
import com.energytracker.service.general.GeneralDeviceServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
	@Cacheable("activedevices")
	public List<CommercialConsumer> getByStartTimeBefore(Instant startTimeBefore) {
		return repository.findByStartTimeBefore(startTimeBefore);
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
