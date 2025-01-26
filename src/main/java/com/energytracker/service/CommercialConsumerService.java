package com.energytracker.service;

import com.energytracker.entity.CommercialConsumer;

/**
 * @author André Heinen
 */
public interface CommercialConsumerService {

	CommercialConsumer getOpenDeviceByDeviceId(Long deviceId);

	void update(CommercialConsumer commercialConsumer);

	void add(CommercialConsumer commercialConsumer);
	void remove(Long deviceId);

}
