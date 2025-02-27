package com.energytracker.repository.devices;

import com.energytracker.entity.devices.Producer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface ProducerRepository extends JpaRepository<Producer, Long> {

	List<Producer> findDevicesByDeviceId(Long deviceId);

}
