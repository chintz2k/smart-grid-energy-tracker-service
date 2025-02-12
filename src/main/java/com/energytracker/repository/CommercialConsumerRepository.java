package com.energytracker.repository;

import com.energytracker.entity.CommercialConsumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface CommercialConsumerRepository extends JpaRepository<CommercialConsumer, Long> {

	List<CommercialConsumer> findDevicesByDeviceId(Long deviceId);

}
