package com.energytracker.repository.devices;

import com.energytracker.entity.devices.CommercialConsumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * @author André Heinen
 */
public interface CommercialConsumerRepository extends JpaRepository<CommercialConsumer, Long> {

	List<CommercialConsumer> findDevicesByDeviceId(Long deviceId);

	List<CommercialConsumer> findByStartTimeBefore(Instant startTimeBefore);

}
