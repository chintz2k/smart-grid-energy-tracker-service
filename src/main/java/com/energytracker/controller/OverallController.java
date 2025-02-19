package com.energytracker.controller;

import com.energytracker.influx.InfluxOverallService;
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
@RequestMapping("/api/overall")
public class OverallController {

	private final InfluxOverallService overallService;

	@Autowired
	public OverallController(InfluxOverallService overallService) {
		this.overallService = overallService;
	}

	@RequestMapping("/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getOverallMeasurementsForChartJs(
			@RequestParam String type,
			@RequestParam(defaultValue = "10m") String range,
			@RequestParam(defaultValue = "none") String aggregation
	) {
		Map<String, Object> mapForChartJs = overallService.getOverallMeasurementsForChartJs(type, range, aggregation);

		return ResponseEntity.ok().body(mapForChartJs);
	}
}
