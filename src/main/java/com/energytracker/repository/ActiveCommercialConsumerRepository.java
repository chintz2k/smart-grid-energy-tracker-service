package com.energytracker.repository;

import com.energytracker.entity.ActiveConsumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author André Heinen
 */
public interface ActiveCommercialConsumerRepository extends JpaRepository<ActiveConsumer, Long> {

	Optional<ActiveConsumer> findByDeviceId(Long deviceId);

	void deleteByDeviceId(Long deviceId);

}
