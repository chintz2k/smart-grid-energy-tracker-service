package com.energytracker.controller;

import com.energytracker.influx.InfluxOwnerService;
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
@RequestMapping("/api/owners")
public class OwnerController {

	private final InfluxOwnerService ownerService;

	@Autowired
	public OwnerController(InfluxOwnerService ownerService) {
		this.ownerService = ownerService;
	}

	@RequestMapping("/{ownerId}/chart")
	public ResponseEntity<Map<String, Object>> getOwnerMeasurementsForChartJs(
			@PathVariable Long ownerId,
			@RequestParam String type,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String aggregationTime,
			@RequestParam(required = false) String aggregationType
	) {
		Map<String, Object> mapforChartJs = ownerService.getOwnerMeasurementsForChartJs(ownerId, type, range, aggregationTime, aggregationType);

		return ResponseEntity.ok().body(mapforChartJs);
	}
}
