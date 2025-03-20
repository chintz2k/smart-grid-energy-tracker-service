package com.energytracker.influx.service.devices;

import com.energytracker.influx.util.ChartJsHelper;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.security.SecurityService;
import com.influxdb.query.FluxRecord;
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
public class InfluxStorageService {

	private final SecurityService securityService;
	private final InfluxQueryHelper influxQueryHelper;
	private final ChartJsHelper chartJsHelper;

	@Autowired
	public InfluxStorageService(SecurityService securityService, InfluxQueryHelper influxQueryHelper, ChartJsHelper chartJsHelper) {
		this.securityService = securityService;
		this.influxQueryHelper = influxQueryHelper;
		this.chartJsHelper = chartJsHelper;
	}

	public Map<String, Object> getStoragesMeasurementsForChartJs(
			String deviceId,
			String field,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType
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
		filterMap = getValidField(field, filterMap);

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_STORAGE,
				range,
				start,
				end,
				List.of(
						InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL,
						InfluxConstants.MEASUREMENT_NAME_STORAGE
				),
				ownerIds,
				deviceIds,
				filterMap,
				aggregateWindowTime,
				aggregateWindowType
		);

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}

	public Map<String, Object> getStoragesByOwnerMeasurementsForChartJs(
			String ownerId,
			String field,
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
				ownerIds.add(Long.parseLong(ownerId));
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
		filterMap = getValidField(field, filterMap);

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_STORAGE,
				range,
				start,
				end,
				List.of(
						InfluxConstants.MEASUREMENT_NAME_STORAGE_OWNER
				),
				ownerIds,
				null,
				filterMap,
				aggregateWindowTime,
				aggregateWindowType
		);

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}

	public Map<String, Object> getStoragesOverallMeasurementsForChartJs(
			String status,
			String field,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType
	) {
		securityService.checkIfUserIsAdminOrIsSystem();

		String measurementName = InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_TOTAL;
		if (status != null && !status.isBlank()) {
			if (status.equals("commercial") || status.equals("kommerziell") || status.equals("commercial storage")) {
				measurementName = InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_COMMERCIAL;
			} else if (status.equals("private") || status.equals("privat") || status.equals("private storage")){
				measurementName = InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_PRIVATE;
			}
		}

		Map<String, String> filterMap = new HashMap<>();
		filterMap = getValidField(field, filterMap);

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_STORAGE,
				range,
				start,
				end,
				List.of(
						measurementName
				),
				null,
				null,
				filterMap,
				aggregateWindowTime,
				aggregateWindowType
		);

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}

	private Map<String, String> getValidField(String field, Map<String, String> filterMap) {
		if (field != null && !field.isBlank()) {
			if (field.equals("capacity")) {
				filterMap.put("_field", "capacity");
			}
			if (field.equals("currentCharge")) {
				filterMap.put("_field", "currentCharge");
			}
		}
		return filterMap;
	}

	public Map<String, Double> getLatestSummaryOfStoragesByOwner(Long ownerId) {
		if (ownerId == null) {
			ownerId = securityService.getCurrentUserId();
		} else {
			if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
				securityService.checkIfUserIsOwnerOrIsAdminOrIsSystem(ownerId);
			}
		}
		String query = String.format("""
        from(bucket: "%s")
          |> range(start: -30d)
          |> filter(fn: (r) => r._measurement == "%s" or r._measurement == "%s")
          |> filter(fn: (r) => r.ownerId == "%s")
          |> filter(fn: (r) => r._field == "currentCharge" or r._field == "capacity")
          |> group(columns: ["_field", "deviceId"])
          |> last()
          |> group(columns: ["_field"])
        """, InfluxConstants.BUCKET_STORAGE, InfluxConstants.MEASUREMENT_NAME_STORAGE, InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL, ownerId);

		return getSummedUpMapFromQuery(query);
	}

	public Map<String, Double> getLatestSummaryOfStoragesTotal() {
		securityService.checkIfUserIsAdminOrIsSystem();
		String query = String.format("""
        from(bucket: "%s")
          |> range(start: -30d)
          |> filter(fn: (r) => r._measurement == "%s" or r._measurement == "%s")
          |> filter(fn: (r) => r._field == "currentCharge" or r._field == "capacity")
          |> group(columns: ["_field", "deviceId"])
          |> last()
          |> group(columns: ["_field"])
        """, InfluxConstants.BUCKET_STORAGE, InfluxConstants.MEASUREMENT_NAME_STORAGE, InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL);

		return getSummedUpMapFromQuery(query);
	}

	private Map<String, Double> getSummedUpMapFromQuery(String query) {
		Map<String, Double> result = new HashMap<>();
		List<FluxTable> tables = influxQueryHelper.executeFluxQuery(query);
		for (FluxTable table : tables) {
			for (FluxRecord record : table.getRecords()) {
				String key = (String) record.getValueByKey("_field");
				Double value = (Double) record.getValueByKey("_value");
				if (key == null || value == null) {
					continue;
				}
				result.merge(key, value, Double::sum);
			}
		}
		return result;
	}
}
