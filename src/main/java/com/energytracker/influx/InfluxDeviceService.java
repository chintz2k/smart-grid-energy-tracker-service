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
public class InfluxDeviceService {

	private final SecurityService securityService;
	private final InfluxDBService influxDBService;
	private final InfluxServiceHelper influxServiceHelper;

	@Autowired
	public InfluxDeviceService(SecurityService securityService, InfluxDBService influxDBService, InfluxServiceHelper influxServiceHelper) {
		this.securityService = securityService;
		this.influxDBService = influxDBService;
		this.influxServiceHelper = influxServiceHelper;
	}

	public Map<String, Object> getDeviceMeasurementsForChartJs(Long deviceId, String type, String range, String aggregateWindowTime, String aggregateWindowType) {
		Long ownerId = securityService.getCurrentUserId();
		if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
			if (!ownerId.equals(influxDBService.getDeviceOwnerId(deviceId))) {
				throw new UnauthorizedAccessException("Unauthorized access to device with ID " + deviceId + " for user with ID " + ownerId);
			}
		}

		String lowerCaseType = type.toLowerCase();
		if (lowerCaseType.equals("commercial_consumer")) {
			type = "consumer";
		}
		if (lowerCaseType.equals("commercial_producer")) {
			type = "producer";
		}

		String fluxQuery = switch (type.toLowerCase()) {
			case "consumer" -> influxServiceHelper.createInfluxQuery(
					InfluxConstants.BUCKET_CONSUMPTION,
					range,
					InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_DEVICE,
					Map.of("deviceId", deviceId.toString()),
					aggregateWindowTime,
					aggregateWindowType
			);
			case "producer" -> influxServiceHelper.createInfluxQuery(
					InfluxConstants.BUCKET_PRODUCTION,
					range,
					InfluxConstants.MEASUREMENT_NAME_PRODUCTION_DEVICE,
					Map.of("deviceId", deviceId.toString()),
					aggregateWindowTime,
					aggregateWindowType
			);
			case "storage" -> influxServiceHelper.createInfluxQuery(
					InfluxConstants.BUCKET_STORAGE,
					range,
					InfluxConstants.MEASUREMENT_NAME_STORAGE,
					Map.of("deviceId", deviceId.toString()),
					aggregateWindowTime,
					aggregateWindowType
			);
			case "commercial_storage" -> influxServiceHelper.createInfluxQuery(
					InfluxConstants.BUCKET_STORAGE,
					range,
					InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL,
					Map.of("deviceId", deviceId.toString()),
					aggregateWindowTime,
					aggregateWindowType
			);
			default -> throw new IllegalArgumentException("Unsupported measurement type: " + type);
		};

		return influxServiceHelper.createMapForChartJsFromQuery(fluxQuery);
	}
}
