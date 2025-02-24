package com.energytracker.influx.service;

import com.energytracker.influx.util.ChartJsHelper;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.security.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class InfluxConsumerService {

	private final SecurityService securityService;
	private final InfluxQueryHelper influxQueryHelper;
	private final ChartJsHelper chartJsHelper;

	@Autowired
	public InfluxConsumerService(SecurityService securityService, InfluxQueryHelper influxQueryHelper, ChartJsHelper chartJsHelper) {
		this.securityService = securityService;
		this.influxQueryHelper = influxQueryHelper;
		this.chartJsHelper = chartJsHelper;
	}

	public Map<String, Object> getConsumersMeasurementsForChartJs(
			String deviceId,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType
	) {
		Long ownerId = securityService.getCurrentUserId();

		List<Long> deviceIds = securityService.getDevicesThatBelongToUserFromStringOrIsAdminOrIsSystem(deviceId);

		List<Long> ownerIds = new ArrayList<>();
		if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
			ownerIds.add(ownerId);
		}

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_CONSUMPTION,
				range,
				start,
				end,
				List.of(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_DEVICE),
				ownerIds,
				deviceIds,
				null,
				aggregateWindowTime,
				aggregateWindowType
		);

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}

	public Map<String, Object> getConsumersByOwnerMeasurementsForChartJs(
			String ownerId,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType
	) {
		List<Long> ownerIds = new ArrayList<>();
		if (ownerId != null) {
			if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
				securityService.checkIfUserIsOwnerOrIsAdminOrIsSystem(Long.parseLong(ownerId));
				ownerIds.add(Long.valueOf(ownerId));
			} else {
				String[] idArray = ownerId.split(",");
				for (String id : idArray) {
					String trimmedId = id.trim();
					ownerIds.add(Long.valueOf(trimmedId));
				}
			}
		} else {
			if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
				ownerIds.add(securityService.getCurrentUserId());
			}
		}

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_CONSUMPTION,
				range,
				start,
				end,
				List.of(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_OWNER),
				ownerIds,
				null,
				null,
				aggregateWindowTime,
				aggregateWindowType
		);

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}

	public Map<String, Object> getConsumersOverallMeasurementsForChartJs(
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType
	) {
		securityService.checkIfUserIsAdminOrIsSystem();

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_CONSUMPTION,
				range,
				start,
				end,
				List.of(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_TOTAL),
				null,
				null,
				null,
				aggregateWindowTime,
				aggregateWindowType
		);

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}
}
