package com.energytracker.influx.service.devices;

import com.energytracker.influx.util.ChartJsHelper;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.security.SecurityService;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class InfluxDeviceService {

	private final InfluxQueryHelper influxQueryHelper;
	private final ChartJsHelper chartJsHelper;
	private final SecurityService securityService;

	public InfluxDeviceService(InfluxQueryHelper influxQueryHelper, ChartJsHelper chartJsHelper, SecurityService securityService) {
		this.influxQueryHelper = influxQueryHelper;
		this.chartJsHelper = chartJsHelper;
		this.securityService = securityService;
	}

	public Map<String, Object> getDevicesOverallMeasurementsForChartJs(
			Long userId,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType
	) {
		String query;
		if (userId != null) {
			query = influxQueryHelper.createInfluxQuery(
					InfluxConstants.BUCKET_CONSUMPTION,
					range,
					start,
					end,
					List.of(
							InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_OWNER,
							InfluxConstants.MEASUREMENT_NAME_PRODUCTION_OWNER
					),
					List.of(userId),
					null,
					null,
					aggregateWindowTime,
					aggregateWindowType
			);
		} else {
			query = influxQueryHelper.createInfluxQuery(
					InfluxConstants.BUCKET_CONSUMPTION,
					range,
					start,
					end,
					List.of(
							InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_TOTAL,
							InfluxConstants.MEASUREMENT_NAME_PRODUCTION_TOTAL
					),
					null,
					null,
					null,
					aggregateWindowTime,
					aggregateWindowType
			);
		}
		return chartJsHelper.createMapForChartJsFromQuery(query, true);
	}

	public Map<String, Object> getPrivateOverallMeasurementsForChartJs(
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType
	) {
		Long userId = securityService.getCurrentUserId();
		String query;
		query = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_CONSUMPTION,
				range,
				start,
				end,
				List.of(
						InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_OWNER,
						InfluxConstants.MEASUREMENT_NAME_PRODUCTION_OWNER
				),
				List.of(userId),
				null,
				null,
				aggregateWindowTime,
				aggregateWindowType
		);
		return chartJsHelper.createMapForChartJsFromQuery(query, true);
	}

	public Map<String, Double> getCurrentAndAveragePrivate(String measurement) {
		Long userId = securityService.getCurrentUserId();
		return Map.of(
				"current", getCurrent(measurement, userId),
				"average", getAverageLast24h(measurement, userId)
		);
	}

	public Map<String, Double> getCurrentStorageDataPrivate() {
		Long userId = securityService.getCurrentUserId();
		return getCurrentStorageData(userId);
	}

	public Map<String, Double> getCurrentAndAverage(String measurement) {
		return Map.of(
				"current", getCurrent(measurement, null),
				"average", getAverageLast24h(measurement, null)
		);
	}

	public Map<String, Double> getCurrentAndAverageByOwner(String measurement, Long userId) {
		return Map.of(
				"current", getCurrent(measurement, userId),
				"average", getAverageLast24h(measurement, userId)
		);
	}

	public Map<String, Double> getCurrentStorageData(Long userId) {
		Map<String, Double> currentStorageData = new HashMap<>();
		String query;
		if (userId != null) {
			query = String.format(
					"from(bucket: \"%s\")" +
							"|> range(start: -2h)" +
							"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
							"|> filter(fn: (r) => (r[\"ownerId\"] == \"%s\"))" +
							"|> group(columns: [\"_field\"])" +
							"|> last()"
					, InfluxConstants.BUCKET_STORAGE, InfluxConstants.MEASUREMENT_NAME_STORAGE_OWNER, userId
			);
		} else {
			query = String.format(
					"from(bucket: \"%s\")" +
							"|> range(start: -2h)" +
							"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
							"|> group(columns: [\"_field\"])" +
							"|> last()"
					, InfluxConstants.BUCKET_STORAGE, InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_TOTAL
			);
		}
		List<FluxTable> tables = influxQueryHelper.executeFluxQuery(query);
		for (FluxTable table : tables) {
			for (FluxRecord record : table.getRecords()) {
				currentStorageData.put(
						(String) record.getValueByKey("_field"),
						(Double) record.getValueByKey("_value")
				);
			}
		}
		return currentStorageData;
	}

	private double getCurrent(String measurement, Long userId) {
		String query;
		if (userId != null) {
			query = String.format(
					"from(bucket: \"%s\")" +
							"|> range(start: -2h)" +
							"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
							"|> filter(fn: (r) => (r[\"ownerId\"] == \"%s\"))" +
							"|> group(columns: [\"_field\"])" +
							"|> last()"
					, InfluxConstants.BUCKET_NET, measurement, userId
			);
		} else {
			query = String.format(
					"from(bucket: \"%s\")" +
							"|> range(start: -2h)" +
							"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
							"|> group(columns: [\"_field\"])" +
							"|> last()"
					, InfluxConstants.BUCKET_NET, measurement
			);
		}
		List<FluxTable> tables = influxQueryHelper.executeFluxQuery(query);
		for (FluxTable table : tables) {
			for (FluxRecord record : table.getRecords()) {
				Object value = record.getValueByKey("_value");
				if (value instanceof Double) {
					return (Double) value;
				}
			}
		}
		return 0;
	}

	private double getAverageLast24h(String measurement, Long userId) {
		String query;
		if (userId != null) {
			query = String.format(
					"from(bucket: \"%s\")" +
							"|> range(start: -24h)" +
							"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
							"|> filter(fn: (r) => (r[\"ownerId\"] == \"%s\"))" +
							"|> group(columns: [\"_field\"])"
					, InfluxConstants.BUCKET_NET, measurement, userId
			);
		} else {
			query = String.format(
					"from(bucket: \"%s\")" +
							"|> range(start: -24h)" +
							"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
							"|> group(columns: [\"_field\"])"
					, InfluxConstants.BUCKET_NET, measurement
			);
		}

		double sum = 0;
		int count = 0;

		List<FluxTable> tables = influxQueryHelper.executeFluxQuery(query);
		for (FluxTable table : tables) {
			for (FluxRecord record : table.getRecords()) {
				Object value = record.getValueByKey("_value");
				if (value instanceof Double) {
					sum += (Double) value;
					count++;
				}
			}
		}

		return count > 0 ? sum / count : 0;
	}
}
