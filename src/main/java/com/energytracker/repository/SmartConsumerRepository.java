package com.energytracker.repository;

import com.energytracker.entity.SmartConsumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface SmartConsumerRepository extends JpaRepository<SmartConsumer, Long> {

	List<SmartConsumer> findDevicesByDeviceId(Long deviceId);

	void deleteByDeviceId(Long deviceId);

}
