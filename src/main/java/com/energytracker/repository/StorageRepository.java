package com.energytracker.repository;

import com.energytracker.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author André Heinen
 */
public interface StorageRepository extends JpaRepository<Storage, Long> {

	List<Storage> findDevicesByDeviceId(Long deviceId);

}
