package com.energytracker.influx.service.net;

import com.energytracker.influx.util.ChartJsHelper;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.security.SecurityService;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class InfluxNetBalanceService {

	private final SecurityService securityService;
	private final InfluxQueryHelper influxQueryHelper;
	private final ChartJsHelper chartJsHelper;

	@Autowired
	public InfluxNetBalanceService(SecurityService securityService, InfluxQueryHelper influxQueryHelper, ChartJsHelper chartJsHelper) {
		this.securityService = securityService;
		this.influxQueryHelper = influxQueryHelper;
		this.chartJsHelper = chartJsHelper;
	}

	public Map<String, Double> getCurrentNetBalance() {
		Map<String, Double> currentBalanceMap = new HashMap<>();
		String fluxQuery = String.format(
				"from(bucket: \"%s\")" +
						"|> range(start: -14d)" +
						"|> filter(fn: (r) => (r[\"_measurement\"] == \"%s\"))" +
						"|> group(columns: [\"_field\"])" +
						"|> last()"
				, InfluxConstants.BUCKET_NET, InfluxConstants.MEASUREMENT_NAME_NET
		);
		List<FluxTable> tables = influxQueryHelper.executeFluxQuery(fluxQuery);
		for (FluxTable table : tables) {
			for (FluxRecord record : table.getRecords()) {
				currentBalanceMap.put(
						(String) record.getValueByKey("_field"),
						(Double) record.getValueByKey("_value")
				);
			}
		}
		return currentBalanceMap;
	}

	public Map<String, Object> getNetBalanceMeasurementsForChartJs(
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		securityService.checkIfUserIsAdminOrIsSystem();

		String fluxQuery = influxQueryHelper.createInfluxQuery(
				InfluxConstants.BUCKET_NET,
				range,
				start,
				end,
				List.of(InfluxConstants.MEASUREMENT_NAME_NET),
				null,
				null,
				null,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}
}
