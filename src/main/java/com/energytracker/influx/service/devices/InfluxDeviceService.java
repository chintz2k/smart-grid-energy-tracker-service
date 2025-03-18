package com.energytracker.influx.service.devices;

import com.energytracker.influx.util.ChartJsHelper;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.security.SecurityService;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class InfluxDeviceService {

	private final SecurityService securityService;
	private final InfluxQueryHelper influxQueryHelper;
	private final ChartJsHelper chartJsHelper;

	public InfluxDeviceService(SecurityService securityService, InfluxQueryHelper influxQueryHelper, ChartJsHelper chartJsHelper) {
		this.securityService = securityService;
		this.influxQueryHelper = influxQueryHelper;
		this.chartJsHelper = chartJsHelper;
	}

	public Map<String, Object> getDevicesOverallMeasurementsForChartJs(
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

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}

	public Map<String, Double> getOverallAndAverageConsumption() {
		return Map.of(
				"overallConsumption", getOverallConsumption(),
				"averageConsumption", getAverageConsumptionLast24h()
		);
	}

	private double getOverallConsumption() {
		String overallConsumptionFluxQuery = String.format(
				"from(bucket: \"%s\")" +
						"|> range(start: -2h)" +
						"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
						"|> group(columns: [\"_field\"])" +
						"|> last()"
				, InfluxConstants.BUCKET_NET, InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_TOTAL
		);
		List<FluxTable> overallConsumptionTables = influxQueryHelper.executeFluxQuery(overallConsumptionFluxQuery);
		for (FluxTable table : overallConsumptionTables) {
			for (FluxRecord record : table.getRecords()) {
				Object value = record.getValueByKey("_value");
				if (value instanceof Double) {
					return (Double) value;
				}
			}
		}
		return 0;
	}

	private double getAverageConsumptionLast24h() {
		String averageConsumptionFluxQuery = String.format(
				"from(bucket: \"%s\")" +
						"|> range(start: -24h)" +
						"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
						"|> group(columns: [\"_field\"])"
				, InfluxConstants.BUCKET_NET, InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_TOTAL
		);

		double sum = 0;
		int count = 0;

		List<FluxTable> averageConsumptionTables = influxQueryHelper.executeFluxQuery(averageConsumptionFluxQuery);
		for (FluxTable table : averageConsumptionTables) {
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
