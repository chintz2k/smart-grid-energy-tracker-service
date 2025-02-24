package com.energytracker.controller.quartz;

import com.energytracker.dto.QuartzCurrentStatsResponse;
import com.energytracker.dto.QuartzMaxStatsResponse;
import com.energytracker.service.QuartzJobMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

	@GetMapping("/running")
	public ResponseEntity<List<String>> getRunningJobs() {
		List<String> runningJobs = service.getRunningJobNames();
		return ResponseEntity.ok().body(runningJobs);
	}

	@GetMapping("/reset")
	public ResponseEntity<Map<String, String>> resetStats() {
		Map<String, String> response = service.resetStats();
		return ResponseEntity.ok().body(response);
	}

}
