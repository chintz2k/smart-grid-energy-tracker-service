package com.energytracker.repository.devices;

import com.energytracker.entity.devices.Storage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface StorageRepository extends JpaRepository<Storage, Long> {

	List<Storage> findDevicesByDeviceId(Long deviceId);

}
