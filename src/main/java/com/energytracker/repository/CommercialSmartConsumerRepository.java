package com.energytracker.repository;

import com.energytracker.entity.CommercialSmartConsumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface CommercialSmartConsumerRepository extends JpaRepository<CommercialSmartConsumer, Long> {

	List<CommercialSmartConsumer> findDevicesByDeviceId(Long deviceId);

}
