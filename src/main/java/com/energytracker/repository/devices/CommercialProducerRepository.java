package com.energytracker.repository.devices;

import com.energytracker.entity.devices.CommercialProducer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * @author André Heinen
 */
public interface CommercialProducerRepository extends JpaRepository<CommercialProducer, Long> {

	List<CommercialProducer> findDevicesByDeviceId(Long deviceId);

	List<CommercialProducer> findByStartTimeBefore(Instant startTimeBefore);

}
