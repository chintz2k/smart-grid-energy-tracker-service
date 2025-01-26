package com.energytracker.service;

import com.energytracker.entity.CommercialProducer;

/**
 * @author André Heinen
 */
public interface CommercialProducerService {

	CommercialProducer getOpenDeviceByDeviceId(Long deviceId);

	void update(CommercialProducer commercialProducer);

	void add(CommercialProducer commercialProducer);
	void remove(Long deviceId);

}
