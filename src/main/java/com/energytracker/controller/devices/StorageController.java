package com.energytracker.controller.devices;

import com.energytracker.influx.service.devices.InfluxStorageService;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@RestController
@RequestMapping("/api/storages")
public class StorageController {

	private final InfluxStorageService storageService;

	@Autowired
	public StorageController(InfluxStorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/devices")
	public ResponseEntity<List<FluxTable>> getStorageMeasurementsByDevice(
			@RequestParam(required = false) String deviceId,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		List<FluxTable> tables = storageService.getStorageMeasurementsByDevice(
				deviceId,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(tables);
	}

	@GetMapping("/owner")
	public ResponseEntity<List<FluxTable>> getStorageMeasurementsByOwner(
			@RequestParam(required = false) String ownerId,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		List<FluxTable> tables = storageService.getStorageMeasurementsByOwner(
				ownerId,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(tables);
	}

	@GetMapping("/overall")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<List<FluxTable>> getStorageMeasurementsTotal(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		List<FluxTable> tables = storageService.getStorageMeasurementsTotal(
				status,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(tables);
	}

	@GetMapping("/devices/chart")
	public ResponseEntity<Map<String, Object>> getStorageMeasurementsByDeviceForChartJs(
			@RequestParam(required = false) String deviceId,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapforChartJs = storageService.getStorageMeasurementsByDeviceForChartJs(
				deviceId,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/owner/chart")
	public ResponseEntity<Map<String, Object>> getStorageMeasurementsByOwnerForChartJs(
			@RequestParam(required = false) String ownerId,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapforChartJs = storageService.getStorageMeasurementsByOwnerForChartJs(
				ownerId,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/overall/chart")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Object>> getStorageMeasurementsTotalForChartJs(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String field,
			@RequestParam(required = false) String range,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@RequestParam(required = false) String aggregateWindowTime,
			@RequestParam(required = false) String aggregateWindowType,
			@RequestParam(required = false) boolean fillMissingValues
	) {
		Map<String, Object> mapforChartJs = storageService.getStorageMeasurementsTotalForChartJs(
				status,
				field,
				range,
				start,
				end,
				aggregateWindowTime,
				aggregateWindowType,
				fillMissingValues
		);

		return ResponseEntity.ok().body(mapforChartJs);
	}

	@GetMapping("/owner/summary")
	public ResponseEntity<Map<String, Double>> getSummaryByOwner(
			@RequestParam(required = false) Long ownerId
	) {
		Map<String, Double> result = storageService.getLatestSummaryOfStoragesByOwner(ownerId);
		return ResponseEntity.ok().body(result);
	}

	@GetMapping("/overall/summary")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
	public ResponseEntity<Map<String, Double>> getOverallSummary() {
		Map<String, Double> result = storageService.getLatestSummaryOfStoragesTotal();
		return ResponseEntity.ok().body(result);
	}
}
