package com.energytracker.service;

import com.energytracker.influx.service.InfluxNetBalanceService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author André Heinen
 */
@Service
public class NetBalanceServiceImpl implements NetBalanceService {

	private static final double MINIMUM_FOR_FOSSIL = 1000000.0;
	private static final double MAXIMUM_FOR_FOSSIL = 1000000000.0;

	private static final double MINIMUM_FOR_RENEWABLE = 1000000000.0;
	private static final double MAXIMUM_FOR_RENEWABLE = 6000000000.0;

	public static final String CACHE_KEY_FOR_FOSSIL = "currentLimitForFossil";
	public static final String CACHE_KEY_FOR_RENEWABLE = "currentLimitForRenewable";

	private final InfluxNetBalanceService influxNetBalanceService;

	private final ConcurrentHashMap<String, Double> cache = new ConcurrentHashMap<>();

	public NetBalanceServiceImpl(InfluxNetBalanceService influxNetBalanceService) {
		this.influxNetBalanceService = influxNetBalanceService;
	}

	@Override
	public synchronized Map<String, Double> setNewCommercialPowerPlantLimit() {

		Map<String, Double> currentBalance = influxNetBalanceService.getCurrentBalance();
		double lastNetBalance = currentBalance.get("currentBalance");

		double newFossilLimit;
		double newRenewableLimit;
		if (lastNetBalance < MINIMUM_FOR_FOSSIL) {
			newFossilLimit = 1.0;
		} else if (lastNetBalance > MAXIMUM_FOR_FOSSIL) {
			newFossilLimit = 0.0;
		} else {
			double difference = MAXIMUM_FOR_FOSSIL - MINIMUM_FOR_FOSSIL;
			double percentOfMaximum = (lastNetBalance - MINIMUM_FOR_FOSSIL) / difference;
			newFossilLimit = 1.0 - percentOfMaximum;
		}
		if (lastNetBalance < MINIMUM_FOR_RENEWABLE) {
			newRenewableLimit = 1.0;
		} else if (lastNetBalance > MAXIMUM_FOR_RENEWABLE) {
			newRenewableLimit = 0.0;
		} else {
			double difference = MAXIMUM_FOR_RENEWABLE - MINIMUM_FOR_RENEWABLE;
			double percentOfMaximum = (lastNetBalance - MINIMUM_FOR_RENEWABLE) / difference;
			newRenewableLimit = 1.0 - percentOfMaximum;
		}

		cache.put(CACHE_KEY_FOR_FOSSIL, newFossilLimit);
		cache.put(CACHE_KEY_FOR_RENEWABLE, newRenewableLimit);

		return Map.of(CACHE_KEY_FOR_FOSSIL, newFossilLimit, CACHE_KEY_FOR_RENEWABLE, newRenewableLimit);
	}

	@Override
	public double getCommercialPowerPlantLimitForFossil() {
		return cache.getOrDefault(CACHE_KEY_FOR_FOSSIL, influxNetBalanceService.getCurrentCommercialPowerPlantLimit());
	}

	@Override
	public double getCommercialPowerPlantLimitForRenewable() {
		return cache.getOrDefault(CACHE_KEY_FOR_RENEWABLE, influxNetBalanceService.getCurrentCommercialPowerPlantLimit());
	}
}
