package com.energytracker.controller.net;

import com.energytracker.influx.service.net.InfluxPowerPlantLimitsService;
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
@RequestMapping("/api/powerplantlimits")
public class PowerPlantLimitsController {

	private final InfluxPowerPlantLimitsService powerPlantLimitsService;

	@Autowired
	public PowerPlantLimitsController(InfluxPowerPlantLimitsService powerPlantLimitsService) {
		this.powerPlantLimitsService = powerPlantLimitsService;
	}

	@GetMapping("/current")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getCurrentCommercialPowerPlantLimits() {
		Map<String, Double> result = powerPlantLimitsService.getCurrentCommercialPowerPlantLimit();
		return ResponseEntity.ok().body(result);
	}

	@GetMapping("/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getPowerPlantLimitsMeasurementsForChartJs(
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapForChartJs = powerPlantLimitsService.getPowerPlantLimitsMeasurementsForChartJs(
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(mapForChartJs);
	}
}
