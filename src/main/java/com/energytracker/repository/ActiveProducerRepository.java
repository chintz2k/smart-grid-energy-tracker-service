package com.energytracker.repository;

import com.energytracker.entity.ActiveProducer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author André Heinen
 */
public interface ActiveProducerRepository extends JpaRepository<ActiveProducer, Long> {

	Optional<ActiveProducer> findByDeviceId(Long deviceId);

	void deleteByDeviceId(Long deviceId);

}
