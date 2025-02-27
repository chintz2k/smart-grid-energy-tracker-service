package com.energytracker.service.net;

import com.energytracker.kafka.events.PowerPlantLimitsEvent;

/**
 * @author André Heinen
 */
public interface PowerPlantLimitsService {

	void setNewCommercialPowerPlantLimit(PowerPlantLimitsEvent event);

	double getCommercialPowerPlantLimitForFossil();
	double getCommercialPowerPlantLimitForRenewable();

}
