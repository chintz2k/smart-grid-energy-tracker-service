package com.energytracker.controller.devices;

import com.energytracker.influx.service.devices.InfluxDeviceService;
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
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType
	) {
		Map<String, Object> mapforChartJs = deviceService.getDevicesOverallMeasurementsForChartJs(
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/consumptioncard")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getConsumptionCard() {
		Map<String, Double> map = deviceService.getOverallAndAverageConsumption();
		return ResponseEntity.ok().body(map);
	}
}
