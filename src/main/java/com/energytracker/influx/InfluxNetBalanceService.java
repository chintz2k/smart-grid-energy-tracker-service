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
public class InfluxNetBalanceService {

	private final SecurityService securityService;
	private final InfluxServiceHelper influxServiceHelper;

	@Autowired
	public InfluxNetBalanceService(SecurityService securityService, InfluxServiceHelper influxServiceHelper) {
		this.securityService = securityService;
		this.influxServiceHelper = influxServiceHelper;
	}

	public Map<String, Object> getNetBalanceMeasurementsForChartJs(String range, String aggregateWindowTime, String aggregateWindowType) {
		if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
			throw new UnauthorizedAccessException("Unauthorized access to net balance data for user with ID " + securityService.getCurrentUserId());
		}

		String fluxQuery = influxServiceHelper.createInfluxQuery(
				InfluxConstants.BUCKET_NET,
				range,
				InfluxConstants.MEASUREMENT_NAME_NET,
				null,
				aggregateWindowTime,
				aggregateWindowType
		);

		return influxServiceHelper.createMapForChartJsFromQuery(fluxQuery);
	}
}
