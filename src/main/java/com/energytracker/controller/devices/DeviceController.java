package com.energytracker.controller.devices;

import com.energytracker.influx.service.devices.InfluxDeviceService;
import com.energytracker.influx.util.InfluxConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author André Heinen
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

	private final InfluxDeviceService deviceService;

	@Autowired
	public DeviceController(InfluxDeviceService deviceService) {
		this.deviceService = deviceService;
	}

	@GetMapping("/overall/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getConsumersOverallMeasurementsForChartJs(
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapforChartJs = deviceService.getDevicesOverallMeasurementsForChartJs(
				userId,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);
		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/private/chart")
	public ResponseEntity<Map<String, Object>> getPrivateOverallMeasurementsForChartJs(
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapforChartJs = deviceService.getPrivateOverallMeasurementsForChartJs(
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);
		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/private/consumptioncard")
	public ResponseEntity<Map<String, Double>> getConsumptionCardPrivate() {
		Map<String, Double> map = deviceService.getCurrentAndAveragePrivate(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_OWNER);
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/{userId}/consumptioncard")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getConsumptionCardByOwner(@PathVariable Long userId) {
		Map<String, Double> map = deviceService.getCurrentAndAverageByOwner(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_OWNER, userId);
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/consumptioncard")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getConsumptionCard() {
		Map<String, Double> map = deviceService.getCurrentAndAverage(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_TOTAL);
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/private/productioncard")
	public ResponseEntity<Map<String, Double>> getProductionCardPrivate() {
		Map<String, Double> map = deviceService.getCurrentAndAveragePrivate(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_OWNER);
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/{userId}/productioncard")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getProductionCardByOwner(@PathVariable Long userId) {
		Map<String, Double> map = deviceService.getCurrentAndAverageByOwner(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_OWNER, userId);
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/productioncard")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getProductionCard() {
		Map<String, Double> map = deviceService.getCurrentAndAverage(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_TOTAL);
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/private/storagecard")
	public ResponseEntity<Map<String, Double>> getStorageCardPrivate() {
		Map<String, Double> map = deviceService.getCurrentStorageDataPrivate();
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/{userId}/storagecard")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getStorageCardByOwner(@PathVariable Long userId) {
		Map<String, Double> map = deviceService.getCurrentStorageData(userId);
		return ResponseEntity.ok().body(map);
	}

	@GetMapping("/storagecard")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getStorageCard() {
		Map<String, Double> map = deviceService.getCurrentStorageData(null);
		return ResponseEntity.ok().body(map);
	}
}
