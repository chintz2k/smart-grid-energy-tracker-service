package com.energytracker.influx.service.devices;

import com.energytracker.exception.exceptions.InvalidPowerTypeException;
import com.energytracker.exception.exceptions.InvalidRenewableValueException;
import com.energytracker.influx.util.ChartJsHelper;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.security.SecurityService;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class InfluxProducerService {

	private final SecurityService securityService;
	private final InfluxQueryHelper influxQueryHelper;
	private final ChartJsHelper chartJsHelper;

	@Autowired
	public InfluxProducerService(SecurityService securityService, InfluxQueryHelper influxQueryHelper, ChartJsHelper chartJsHelper) {
		this.securityService = securityService;
		this.influxQueryHelper = influxQueryHelper;
		this.chartJsHelper = chartJsHelper;
	}

	public List<FluxTable> getProductionMeasurementsByDevice(
			String deviceId,
			String powerType,
			String renewable,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		Long ownerId = securityService.getCurrentUserId();

		List<Long> deviceIds;
		if (deviceId == null || deviceId.isBlank()) {
			deviceIds = securityService.getDevicesThatBelongToUserFromStringOrIsAdminOrIsSystem(deviceId);
		} else {
			deviceIds = new ArrayList<>();
			deviceIds.add(Long.valueOf(deviceId));
		}

		List<Long> ownerIds = new ArrayList<>();
		if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
			ownerIds.add(ownerId);
		}

		Map<String, String> filterMap = new HashMap<>();
		filterMap = getValidPowerType(powerType, filterMap);
		filterMap = getValidRenewable(renewable, filterMap);

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_PRODUCTION,
				range,
				start,
				end,
				List.of(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_DEVICE),
				ownerIds,
				deviceIds,
				filterMap,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return influxQueryHelper.executeFluxQuery(fluxQuery);
	}

	public List<FluxTable> getProductionMeasurementsByOwner(
			String ownerId,
			String powerType,
			String renewable,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		List<Long> ownerIds = new ArrayList<>();
		if (ownerId != null) {
			if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
				securityService.checkIfUserIsOwnerOrIsAdminOrIsSystem(Long.parseLong(ownerId));
				ownerIds.add(securityService.getCurrentUserId());
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

		Map<String, String> filterMap = new HashMap<>();
		filterMap = getValidPowerType(powerType, filterMap);
		filterMap = getValidRenewable(renewable, filterMap);

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_PRODUCTION,
				range,
				start,
				end,
				List.of(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_OWNER),
				ownerIds,
				null,
				filterMap,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return influxQueryHelper.executeFluxQuery(fluxQuery);
	}

	public List<FluxTable> getProductionMeasurementsTotal(
			String powerType,
			String renewable,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		securityService.checkIfUserIsAdminOrIsSystem();

		Map<String, String> filterMap = new HashMap<>();

		filterMap = getValidPowerType(powerType, filterMap);
		filterMap = getValidRenewable(renewable, filterMap);

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_PRODUCTION,
				range,
				start,
				end,
				List.of(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_TOTAL),
				null,
				null,
				filterMap,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return influxQueryHelper.executeFluxQuery(fluxQuery);
	}

	public Map<String, Object> getProductionMeasurementsByDeviceForChartJs(
			String deviceId,
			String powerType,
			String renewable,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		return chartJsHelper.createMapForChartJsFromFluxTables(getProductionMeasurementsByDevice(
						deviceId,
						powerType,
						renewable,
						range,
						start,
						end,
						aggregateWindowTime,
						aggregateWindowType,
						fillMissingValues
				),
				true
		);
	}

	public Map<String, Object> getProductionMeasurementsByOwnerForChartJs(
			String ownerId,
			String powerType,
			String renewable,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		return chartJsHelper.createMapForChartJsFromFluxTables(getProductionMeasurementsByOwner(
						ownerId,
						powerType,
						renewable,
						range,
						start,
						end,
						aggregateWindowTime,
						aggregateWindowType,
						fillMissingValues
				),
				true
		);
	}

	public Map<String, Object> getProductionMeasurementsTotalForChartJs(
			String powerType,
			String renewable,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		return chartJsHelper.createMapForChartJsFromFluxTables(getProductionMeasurementsTotal(
						powerType,
						renewable,
						range,
						start,
						end,
						aggregateWindowTime,
						aggregateWindowType,
						fillMissingValues
				),
				true
		);
	}

	private Map<String, String> getValidPowerType(String powerType, Map<String, String> filterMap) {
		if (powerType != null && !powerType.isBlank()) {

			String[] powerTypes = powerType.split(",");
			List<String> validPowerTypes = new ArrayList<>();

			for (String type : powerTypes) {
				String trimmedType = type.trim();
				if (InfluxConstants.POWER_TYPES.contains(trimmedType)) {
					validPowerTypes.add(trimmedType);
				}
				if (trimmedType.equals("Alle Energiearten")) {
					validPowerTypes.add("Alle Energiearten");
				}
			}

			if (!validPowerTypes.isEmpty()) {
				filterMap.put("powerType", String.join(",", validPowerTypes));
			} else {
				throw new InvalidPowerTypeException("Unsupported power types: " + powerType + ". Supported types: " + InfluxConstants.POWER_TYPES);
			}
		}
		return filterMap;
	}

	private Map<String, String> getValidRenewable(String renewable, Map<String, String> filterMap) {
		if (renewable != null) {
			if (renewable.equals("true")) {
				filterMap.put("renewable", "erneuerbar");
			} else if (renewable.equals("false")) {
				filterMap.put("renewable", "nicht erneuerbar");
			} else {
				throw new InvalidRenewableValueException("Unsupported renewable value: " + renewable + ", supported values: true, false");
			}
		}
		return filterMap;
	}
}
