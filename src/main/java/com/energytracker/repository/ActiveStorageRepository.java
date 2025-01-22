package com.energytracker.repository;

import com.energytracker.entity.ActiveStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author André Heinen
 */
public interface ActiveStorageRepository extends JpaRepository<ActiveStorage, Long> {

	Optional<ActiveStorage> findByDeviceId(Long deviceId);

	void deleteByDeviceId(Long deviceId);

}
