package com.energytracker.repository;

import com.energytracker.entity.ActiveSmartConsumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author André Heinen
 */
public interface ActiveSmartConsumerRepository extends JpaRepository<ActiveSmartConsumer, Long> {

	Optional<ActiveSmartConsumer> findByDeviceId(Long deviceId);

	void deleteByDeviceId(Long deviceId);

}
