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
public class InfluxOverallService {

	private final SecurityService securityService;
	private final InfluxServiceHelper influxServiceHelper;

	@Autowired
	public InfluxOverallService(SecurityService securityService, InfluxServiceHelper influxServiceHelper) {
		this.securityService = securityService;
		this.influxServiceHelper = influxServiceHelper;
	}

	public Map<String, Object> getOverallMeasurementsForChartJs(String type, String range, String aggregateWindowTime, String aggregateWindowType) {
		if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
			throw new UnauthorizedAccessException("Unauthorized access to overall data for user with ID " + securityService.getCurrentUserId());
		}

		String fluxQuery = switch (type.toLowerCase()) {
			case "consumption" -> influxServiceHelper.createInfluxQuery(
					InfluxConstants.BUCKET_CONSUMPTION,
					range,
					InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_TOTAL,
					null,
					aggregateWindowTime,
					aggregateWindowType
			);
			case "production" -> influxServiceHelper.createInfluxQuery(
					InfluxConstants.BUCKET_PRODUCTION,
					range,
					InfluxConstants.MEASUREMENT_NAME_PRODUCTION_TOTAL,
					null,
					aggregateWindowTime,
					aggregateWindowType
			);
			default -> throw new IllegalArgumentException("Unsupported measurement type: " + type);
		};

		return influxServiceHelper.createMapForChartJsFromQuery(fluxQuery);
	}
}
