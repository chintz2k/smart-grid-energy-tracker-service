package com.energytracker.influx;

import com.energytracker.exception.UnauthorizedAccessException;
import com.energytracker.security.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class InfluxOwnerService {

	private final SecurityService securityService;
	private final InfluxServiceHelper influxServiceHelper;

	@Autowired
	public InfluxOwnerService(SecurityService securityService, InfluxServiceHelper influxServiceHelper) {
		this.securityService = securityService;
		this.influxServiceHelper = influxServiceHelper;
	}

	public Map<String, Object> getOwnerMeasurementsForChartJs(Long ownerId, String type, String range, String aggregateWindowTime, String aggregateWindowType) {
		if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
			if (!ownerId.equals(securityService.getCurrentUserId())) {
				throw new UnauthorizedAccessException("Unauthorized access to owner with ID " + ownerId + " for user with ID " + securityService.getCurrentUserId());
			}
		}

		String fluxQuery = switch (type.toLowerCase()) {
			case "consumption" -> influxServiceHelper.createInfluxQuery(
					InfluxConstants.BUCKET_CONSUMPTION,
					range,
					InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_OWNER,
					Map.of("ownerId", ownerId.toString()),
					aggregateWindowTime,
					aggregateWindowType
			);
			case "production" -> influxServiceHelper.createInfluxQuery(
					InfluxConstants.BUCKET_PRODUCTION,
					range,
					InfluxConstants.MEASUREMENT_NAME_PRODUCTION_OWNER,
					Map.of("ownerId", ownerId.toString()),
					aggregateWindowTime,
					aggregateWindowType
			);
			default -> throw new IllegalArgumentException("Unsupported measurement type: " + type);
		};

		return influxServiceHelper.createMapForChartJsFromQuery(fluxQuery);
	}
}
