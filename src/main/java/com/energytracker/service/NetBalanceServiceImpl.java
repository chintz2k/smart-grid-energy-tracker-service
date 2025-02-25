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

	private static final double MINIMUM_BALANCE = 1000000000.0;
	private static final double MAXIMUM_BALANCE = 6000000000.0;
	private static final String CACHE_KEY_LIMIT = "currentLimit";

	private final InfluxNetBalanceService influxNetBalanceService;

	private final ConcurrentHashMap<String, Double> cache = new ConcurrentHashMap<>();

	public NetBalanceServiceImpl(InfluxNetBalanceService influxNetBalanceService) {
		this.influxNetBalanceService = influxNetBalanceService;
	}

	@Override
	public double setNewCommercialPowerPlantLimit() {

		Map<String, Double> currentBalance = influxNetBalanceService.getCurrentBalance();
		double lastNetBalance = currentBalance.get("currentBalance");

		double newLimit;
		if (lastNetBalance < MINIMUM_BALANCE) {
			newLimit = 1.0;
		} else if (lastNetBalance > MAXIMUM_BALANCE) {
			newLimit = 0.0;
		} else {
			double difference = MAXIMUM_BALANCE - MINIMUM_BALANCE;
			double percentOfMaximum = (lastNetBalance - MINIMUM_BALANCE) / difference;
			newLimit = 1.0 - percentOfMaximum;
		}

		cache.put(CACHE_KEY_LIMIT, newLimit);
		return newLimit;
	}

	@Override
	public double getCachedCommercialPowerPlantLimit() {
		return cache.getOrDefault(CACHE_KEY_LIMIT, influxNetBalanceService.getCurrentCommercialPowerPlantLimit());
	}
}
