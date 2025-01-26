package com.energytracker.repository;

import com.energytracker.entity.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface ConsumerRepository extends JpaRepository<Consumer, Long> {

	List<Consumer> findDevicesByDeviceId(Long deviceId);

	void deleteByDeviceId(Long deviceId);

}
