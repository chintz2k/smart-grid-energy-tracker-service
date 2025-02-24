package com.energytracker.influx.service;

import com.energytracker.influx.util.ChartJsHelper;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.security.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	public Map<String, Object> getNetBalanceMeasurementsForChartJs(
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType
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
				aggregateWindowType
		);

		return chartJsHelper.createMapForChartJsFromQuery(fluxQuery, true);
	}
}
