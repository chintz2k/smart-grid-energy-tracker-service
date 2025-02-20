package com.energytracker.controller;

import com.energytracker.influx.InfluxNetBalanceService;
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

	@RequestMapping("/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getNetBalanceMeasurementsForChartJs(
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String aggregationTime,
			@RequestParam(required = false) String aggregationType
	) {
		Map<String, Object> mapForChartJs = netBalanceService.getNetBalanceMeasurementsForChartJs(range, aggregationTime, aggregationType);

		return ResponseEntity.ok().body(mapForChartJs);
	}
}
