package com.energytracker.controller;

import com.energytracker.influx.InfluxDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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

	@RequestMapping("/{deviceId}/chart")
	public ResponseEntity<Map<String, Object>> getDeviceMeasurementsForChartJs(
			@PathVariable Long deviceId,
			@RequestParam String type,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String aggregationTime,
			@RequestParam(required = false) String aggregationType
	) {
		Map<String, Object> mapforChartJs = deviceService.getDeviceMeasurementsForChartJs(deviceId, type, range, aggregationTime, aggregationType);

		return ResponseEntity.ok().body(mapforChartJs);
	}
}
