package com.energytracker.service.devices;

import com.energytracker.entity.devices.CommercialProducer;
import com.energytracker.repository.devices.CommercialProducerRepository;
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
public class CommercialProducerServiceImpl extends GeneralDeviceServiceImpl<CommercialProducer, CommercialProducerRepository> {

	public CommercialProducerServiceImpl(CommercialProducerRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable("activedevices")
	public List<CommercialProducer> getByStartTimeBefore(Instant startTimeBefore) {
		return repository.findByStartTimeBefore(startTimeBefore);
	}

	@Override
	@Transactional(readOnly = true)
	public CommercialProducer getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}
}
