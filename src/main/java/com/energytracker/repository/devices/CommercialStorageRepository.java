package com.energytracker.repository.devices;

import com.energytracker.entity.devices.CommercialStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface CommercialStorageRepository extends JpaRepository<CommercialStorage, Long> {

	List<CommercialStorage> findDevicesByDeviceId(Long deviceId);

}
