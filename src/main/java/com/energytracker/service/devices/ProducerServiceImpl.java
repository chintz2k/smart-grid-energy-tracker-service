package com.energytracker.service.devices;

import com.energytracker.entity.devices.Producer;
import com.energytracker.repository.devices.ProducerRepository;
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
public class ProducerServiceImpl extends GeneralDeviceServiceImpl<Producer, ProducerRepository> {

	public ProducerServiceImpl(ProducerRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable("activedevices")
	public List<Producer> getByStartTimeBefore(Instant startTimeBefore) {
		return repository.findByStartTimeBefore(startTimeBefore);
	}

	@Override
	@Transactional(readOnly = true)
	public Producer getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
