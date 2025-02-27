package com.energytracker.service.net;

import com.energytracker.influx.service.net.InfluxPowerPlantLimitsService;
import com.energytracker.kafka.events.PowerPlantLimitsEvent;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author André Heinen
 */
@Service
public class PowerPlantLimitsServiceImpl implements PowerPlantLimitsService {

	private final String CACHE_KEY_FOR_FOSSIL = "currentLimitForFossil";
	private final String CACHE_KEY_FOR_RENEWABLE = "currentLimitForRenewable";

	private final InfluxPowerPlantLimitsService influxPowerPlantLimitsService;

	private final ConcurrentHashMap<String, Double> cache = new ConcurrentHashMap<>();

	public PowerPlantLimitsServiceImpl(InfluxPowerPlantLimitsService influxPowerPlantLimitsService) {
		this.influxPowerPlantLimitsService = influxPowerPlantLimitsService;
	}

	@Override
	public synchronized void setNewCommercialPowerPlantLimit(PowerPlantLimitsEvent event) {
		cache.put(CACHE_KEY_FOR_FOSSIL, event.getFossilLimit());
		cache.put(CACHE_KEY_FOR_RENEWABLE, event.getRenewableLimit());
	}

	@Override
	public double getCommercialPowerPlantLimitForFossil() {
		Double limit = cache.get(CACHE_KEY_FOR_FOSSIL);
		if (limit != null) {
			return limit;
		} else {
			Double influxLimit = influxPowerPlantLimitsService.getCurrentCommercialPowerPlantLimit().get("fossilLimit");
			if (influxLimit != null) {
				cache.put(CACHE_KEY_FOR_FOSSIL, influxLimit);
				return influxLimit;
			}
		}
		return 1.0;
	}

	@Override
	public double getCommercialPowerPlantLimitForRenewable() {
		Double limit = cache.get(CACHE_KEY_FOR_RENEWABLE);
		if (limit != null) {
			return limit;
		} else {
			Double influxLimit = influxPowerPlantLimitsService.getCurrentCommercialPowerPlantLimit().get("renewableLimit");
			if (influxLimit != null) {
				cache.put(CACHE_KEY_FOR_RENEWABLE, influxLimit);
				return influxLimit;
			}
		}
		return 1.0;
	}
}
