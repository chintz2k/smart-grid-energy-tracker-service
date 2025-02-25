package com.energytracker.controller;

import com.energytracker.influx.service.InfluxNetBalanceService;
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
@RequestMapping("/api/netbalance")
public class NetBalanceController {

	private final InfluxNetBalanceService netBalanceService;

	@Autowired
	public NetBalanceController(InfluxNetBalanceService netBalanceService) {
		this.netBalanceService = netBalanceService;
	}

	@RequestMapping("/latest")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getCurrentBalance() {
		Map<String, Double> result = netBalanceService.getCurrentBalance();
		return ResponseEntity.ok().body(result);
	}

	@RequestMapping("/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getNetBalanceMeasurementsForChartJs(
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType
	) {
		Map<String, Object> mapForChartJs = netBalanceService.getNetBalanceMeasurementsForChartJs(
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType
		);

		return ResponseEntity.ok().body(mapForChartJs);
	}
}
