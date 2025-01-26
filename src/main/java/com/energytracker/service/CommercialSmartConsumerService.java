package com.energytracker.service;

import com.energytracker.entity.CommercialSmartConsumer;

/**
 * @author André Heinen
 */
public interface CommercialSmartConsumerService {

	CommercialSmartConsumer getOpenDeviceByDeviceId(Long deviceId);

	void update(CommercialSmartConsumer commercialSmartConsumer);

	void add(CommercialSmartConsumer commercialSmartConsumer);
	void remove(Long deviceId);

}
