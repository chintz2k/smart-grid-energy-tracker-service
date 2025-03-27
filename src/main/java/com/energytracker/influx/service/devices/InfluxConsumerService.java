package com.energytracker.influx.service.devices;

import com.energytracker.influx.util.ChartJsHelper;
import com.energytracker.influx.util.InfluxConstants;
import com.energytracker.influx.util.InfluxQueryHelper;
import com.energytracker.security.SecurityService;
import com.influxdb.query.FluxTable;
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

	public List<FluxTable> getConsumptionMeasurementsByDevice(
			String deviceId,
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
				aggregateWindowType,
				fillMissingValues
		);

		return influxQueryHelper.executeFluxQuery(fluxQuery);
	}

	public List<FluxTable> getConsumptionMeasurementsByOwner(
			String ownerId,
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
				aggregateWindowType,
				fillMissingValues
		);

		return influxQueryHelper.executeFluxQuery(fluxQuery);
	}

	public List<FluxTable> getConsumptionMeasurementsTotal(
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
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
				aggregateWindowType,
				fillMissingValues
		);

		return influxQueryHelper.executeFluxQuery(fluxQuery);
	}

	public Map<String, Object> getConsumptionMeasurementsByDeviceForChartJs(
			String deviceId,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		return chartJsHelper.createMapForChartJsFromFluxTables(getConsumptionMeasurementsByDevice(
						deviceId,
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

	public Map<String, Object> getConsumptionMeasurementsByOwnerForChartJs(
			String ownerId,
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		return chartJsHelper.createMapForChartJsFromFluxTables(getConsumptionMeasurementsByOwner(
						ownerId,
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

	public Map<String, Object> getConsumptionMeasurementsTotalForChartJs(
			String range,
			String start,
			String end,
			String aggregateWindowTime,
			String aggregateWindowType,
			boolean fillMissingValues
	) {
		return chartJsHelper.createMapForChartJsFromFluxTables(getConsumptionMeasurementsTotal(

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
}
