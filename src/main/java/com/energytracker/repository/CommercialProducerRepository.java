package com.energytracker.repository;

import com.energytracker.entity.CommercialProducer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface CommercialProducerRepository extends JpaRepository<CommercialProducer, Long> {

	List<CommercialProducer> findDevicesByDeviceId(Long deviceId);

}
