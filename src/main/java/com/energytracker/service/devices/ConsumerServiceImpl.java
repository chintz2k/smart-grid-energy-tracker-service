package com.energytracker.service.devices;

import com.energytracker.entity.devices.Consumer;
import com.energytracker.repository.devices.ConsumerRepository;
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
public class ConsumerServiceImpl extends GeneralDeviceServiceImpl<Consumer, ConsumerRepository> {

	public ConsumerServiceImpl(ConsumerRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable("activedevices")
	public List<Consumer> getByStartTimeBefore(Instant startTimeBefore) {
		return repository.findByStartTimeBefore(startTimeBefore);
	}

	@Override
	@Transactional(readOnly = true)
	public Consumer getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
