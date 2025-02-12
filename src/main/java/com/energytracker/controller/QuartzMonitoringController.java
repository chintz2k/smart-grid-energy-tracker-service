package com.energytracker.controller;

import com.energytracker.dto.QuartzCurrentStatsResponse;
import com.energytracker.dto.QuartzMaxStatsResponse;
import com.energytracker.service.QuartzJobMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author André Heinen
 */
@RestController
@RequestMapping("/quartz")
public class QuartzMonitoringController {

	private final QuartzJobMonitoringService service;

	@Autowired
	public QuartzMonitoringController(QuartzJobMonitoringService service) {
		this.service = service;
	}

	@GetMapping("/current")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<QuartzCurrentStatsResponse>getCurrentStats() {
		return ResponseEntity.ok().body(service.getCurrentStats());
	}

	@GetMapping("/max")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<QuartzMaxStatsResponse> getMaxStats() {
		return ResponseEntity.ok().body(service.getMaxStats());
	}
}
