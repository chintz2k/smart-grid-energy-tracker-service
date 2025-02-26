package com.energytracker.service;

import java.util.Map;

/**
 * @author André Heinen
 */
public interface NetBalanceService {

	String CACHE_KEY_FOR_FOSSIL = "currentLimitForFossil";
	String CACHE_KEY_FOR_RENEWABLE = "currentLimitForRenewable";

	Map<String, Double> setNewCommercialPowerPlantLimit();
	double getCommercialPowerPlantLimitForFossil();
	double getCommercialPowerPlantLimitForRenewable();

}
