package com.energytracker.controller.devices;

import com.energytracker.influx.service.devices.InfluxStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author André Heinen
 */
@RestController
@RequestMapping("/api/storages")
public class StorageController {

	private final InfluxStorageService storageService;

	@Autowired
	public StorageController(InfluxStorageService storageService) {
		this.storageService = storageService;
	}

	@RequestMapping("/devices/chart")
	public ResponseEntity<Map<String, Object>> getStoragesMeasurementsForChartJs(
			@RequestParam(required = false) String deviceId,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType
	) {
		Map<String, Object> mapforChartJs = storageService.getStoragesMeasurementsForChartJs(
				deviceId,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@RequestMapping("/owner/chart")
	public ResponseEntity<Map<String, Object>> getStoragesByOwnerMeasurementsForChartJs(
			@RequestParam(required = false) String ownerId,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType
	) {
		Map<String, Object> mapforChartJs = storageService.getStoragesByOwnerMeasurementsForChartJs(
				ownerId,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@RequestMapping("/overall/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getStoragesOverallMeasurementsForChartJs(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType
	) {
		Map<String, Object> mapforChartJs = storageService.getStoragesOverallMeasurementsForChartJs(
				status,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@RequestMapping("/owner/summary")
	public ResponseEntity<Map<String, Double>> getSummaryByOwner(
			@RequestParam(required = false) Long ownerId
	) {
		Map<String, Double> result = storageService.getLatestSummaryOfStoragesByOwner(ownerId);
		return ResponseEntity.ok().body(result);
	}

	@RequestMapping("/overall/summary")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getOverallSummary() {
		Map<String, Double> result = storageService.getLatestSummaryOfStoragesTotal();
		return ResponseEntity.ok().body(result);
	}
}
