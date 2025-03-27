package com.energytracker.controller.net;

import com.energytracker.influx.service.net.InfluxNetBalanceService;
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
@RequestMapping("/api/netbalance")
public class NetBalanceController {

	private final InfluxNetBalanceService netBalanceService;

	@Autowired
	public NetBalanceController(InfluxNetBalanceService netBalanceService) {
		this.netBalanceService = netBalanceService;
	}

	@GetMapping("/current")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getCurrentBalance() {
		Map<String, Double> result = netBalanceService.getCurrentNetBalance();
		return ResponseEntity.ok().body(result);
	}

	@GetMapping("/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getNetBalanceMeasurementsForChartJs(
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapForChartJs = netBalanceService.getNetBalanceMeasurementsForChartJs(
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
