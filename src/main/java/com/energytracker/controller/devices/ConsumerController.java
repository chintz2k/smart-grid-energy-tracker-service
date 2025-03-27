package com.energytracker.controller.devices;

import com.energytracker.influx.service.devices.InfluxConsumerService;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@RestController
@RequestMapping("/api/consumers")
public class ConsumerController {

	private final InfluxConsumerService consumerService;

	@Autowired
	public ConsumerController(InfluxConsumerService consumerService) {
		this.consumerService = consumerService;
	}

	@GetMapping("/devices")
	public ResponseEntity<List<FluxTable>> getConsumptionMeasurementsByDevice(
			@RequestParam(required = false) String deviceId,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		List<FluxTable> tables = consumerService.getConsumptionMeasurementsByDevice(
				deviceId,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(tables);
	}

	@GetMapping("/owner")
	public ResponseEntity<List<FluxTable>> getConsumptionMeasurementsByOwner(
			@RequestParam(required = false) String ownerId,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		List<FluxTable> tables = consumerService.getConsumptionMeasurementsByOwner(
				ownerId,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(tables);
	}

	@GetMapping("/overall")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<List<FluxTable>> getConsumptionMeasurementsTotal(
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		List<FluxTable> tables = consumerService.getConsumptionMeasurementsTotal(
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(tables);
	}

	@GetMapping("/devices/chart")
	public ResponseEntity<Map<String, Object>> getConsumersMeasurementsForChartJs(
			@RequestParam(required = false) String deviceId,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapforChartJs = consumerService.getConsumptionMeasurementsByDeviceForChartJs(
				deviceId,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/owner/chart")
	public ResponseEntity<Map<String, Object>> getConsumersByOwnerMeasurementsForChartJs(
			@RequestParam(required = false) String ownerId,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapforChartJs = consumerService.getConsumptionMeasurementsByOwnerForChartJs(
				ownerId,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/overall/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getConsumersOverallMeasurementsForChartJs(
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapforChartJs = consumerService.getConsumptionMeasurementsTotalForChartJs(
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}
}
