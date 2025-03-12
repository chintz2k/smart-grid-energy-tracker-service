package com.energytracker.service.devices;

import com.energytracker.entity.devices.CommercialStorage;
import com.energytracker.repository.devices.CommercialStorageRepository;
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
public class CommercialStorageServiceImpl extends GeneralDeviceServiceImpl<CommercialStorage, CommercialStorageRepository> {

	public CommercialStorageServiceImpl(CommercialStorageRepository repository) {
		super(repository);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable("activedevices")
	public List<CommercialStorage> getByStartTimeBefore(Instant startTimeBefore) {
		return repository.findByStartTimeBefore(startTimeBefore);
	}

	@Override
	@Transactional(readOnly = true)
	public CommercialStorage getOpenDeviceByDeviceId(Long deviceId) {
		return repository.findDevicesByDeviceId(deviceId)
				.stream()
				.filter(device -> device.getEndTime() == null)
				.findFirst()
				.orElse(null);
	}

	@Override
	@Transactional
	public void updateEndTime(Long deviceId, Instant endTime) {
		CommercialStorage device = getOpenDeviceByDeviceId(deviceId);
		if (device != null) {
			repository.delete(device);
		}
	}
}
