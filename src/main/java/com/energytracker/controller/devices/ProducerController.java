package com.energytracker.controller.devices;

import com.energytracker.influx.service.devices.InfluxProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author André Heinen
 */
@RestController
@RequestMapping("/api/producers")
public class ProducerController {

	private final InfluxProducerService producerService;

	@Autowired
	public ProducerController(InfluxProducerService producerService) {
		this.producerService = producerService;
	}

	@GetMapping("/devices/chart")
	public ResponseEntity<Map<String, Object>> getProducersMeasurementsForChartJs(
			@RequestParam(required = false) String deviceId,
			@RequestParam(required = false) String powerType,
			@RequestParam(required = false) String renewable,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType
	) {
		Map<String, Object> mapforChartJs = producerService.getProducersMeasurementsForChartJs(
				deviceId,
				powerType,
				renewable,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/owner/chart")
	public ResponseEntity<Map<String, Object>> getProducersByOwnerMeasurementsForChartJs(
			@RequestParam(required = false) String ownerId,
			@RequestParam(required = false) String powerType,
			@RequestParam(required = false) String renewable,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType
	) {
		Map<String, Object> mapforChartJs = producerService.getProducersByOwnerMeasurementsForChartJs(
				ownerId,
				powerType,
				renewable,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/overall/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getProducersOverallMeasurementsForChartJs(
			@RequestParam(required = false) String powerType,
			@RequestParam(required = false) String renewable,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType
	) {
		Map<String, Object> mapforChartJs = producerService.getProducersOverallMeasurementsForChartJs(
				powerType,
				renewable,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}
}
