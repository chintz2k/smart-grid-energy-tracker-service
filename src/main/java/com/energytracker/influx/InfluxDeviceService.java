package com.energytracker.influx;

import com.energytracker.exception.InvalidTimeFormatForInflux;
import com.energytracker.exception.UnauthorizedAccessException;
import com.energytracker.security.SecurityService;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class InfluxDeviceService {

	private static final Logger logger = LoggerFactory.getLogger(InfluxDeviceService.class);

	private final InfluxDBClient influxDBClient;
	private final SecurityService securityService;
	private final InfluxDBService influxDBService;
	private final ChartColorPicker chartColorPicker;

	@Autowired
	public InfluxDeviceService(InfluxDBClient influxDBClient, SecurityService securityService, InfluxDBService influxDBService, ChartColorPicker chartColorPicker) {
		this.influxDBClient = influxDBClient;
		this.securityService = securityService;
		this.influxDBService = influxDBService;
		this.chartColorPicker = chartColorPicker;
	}

	private String ensureTimeUnit(String input, String defaultUnit) {
		// Prüfen, ob der String *nur* aus Zahlen besteht
		if (input.matches("^\\d+$")) {
			return input + defaultUnit; // Einheit anhängen (m für Range, s für Aggregation)
		}
		// Prüfen, ob der String bereits mit einer gültigen Influx-Zeiteinheit gegeben ist
		if (input.matches("^\\d+[smhdw]$")) {
			return input; // Unverändert zurückgeben
		}
		// Als Fallback: Wenn ungültig, Standard-Zeit anhängen
		throw new InvalidTimeFormatForInflux(input + " ist keine gültige Zeiteinheit. Erwartet wird: dauer + m, s, h, d, w, M, y, n)");
	}

	public Map<String, Object> getDeviceMeasurementsForChartJs(Long deviceId, String type, String range, String aggregation) {
		Long ownerId = securityService.getCurrentUserId();
		if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
			if (!ownerId.equals(influxDBService.getDeviceOwnerId(deviceId))) {
				throw new UnauthorizedAccessException("Unauthorized access to device with ID " + deviceId + " for user with ID " + ownerId);
			}
		}

		range = ensureTimeUnit(range, "m");

		String lowerCaseType = type.toLowerCase();
		if (lowerCaseType.equals("commercial_consumer")) {
			type = "consumer";
		}
		if (lowerCaseType.equals("commercial_producer")) {
			type = "producer";
		}
		String aggregateWindow;
		if (aggregation == null || aggregation.isBlank() || aggregation.equals("none") || aggregation.equals("false") || aggregation.equals("no")) {
			aggregateWindow = "";
		} else {
			aggregation = ensureTimeUnit(aggregation, "s");
			aggregateWindow = "|> aggregateWindow(every: " + aggregation + ", fn: mean) ";
		}

		String fluxQuery = switch (type.toLowerCase()) {
			case "consumer" -> String.format(
					"from(bucket: \"%s\") "
							+ "|> range(start: -%s) "
							+ "|> filter(fn: (r) => r._measurement == \"%s\") "
							+ "|> filter(fn: (r) => r.deviceId == \"%s\") "
							+ aggregateWindow
							+ "|> yield(name: \"mean\")",
					InfluxConstants.BUCKET_CONSUMPTION, range, InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_DEVICE, deviceId
			);
			case "producer" -> String.format(
					"from(bucket: \"%s\") "
							+ "|> range(start: -%s) "
							+ "|> filter(fn: (r) => r._measurement == \"%s\") "
							+ "|> filter(fn: (r) => r.deviceId == \"%s\") "
							+ aggregateWindow
							+ "|> yield(name: \"mean\")",
					InfluxConstants.BUCKET_PRODUCTION, range, InfluxConstants.MEASUREMENT_NAME_PRODUCTION_DEVICE, deviceId
			);
			case "storage" -> String.format(
					"from(bucket: \"%s\") "
							+ "|> range(start: -%s) "
							+ "|> filter(fn: (r) => r._measurement == \"%s\") "
							+ "|> filter(fn: (r) => r.deviceId == \"%s\") "
							+ aggregateWindow
							+ "|> yield(name: \"mean\")",
					InfluxConstants.BUCKET_STORAGE, range, InfluxConstants.MEASUREMENT_NAME_STORAGE, deviceId
			);
			case "commercial_storage" -> String.format(
					"from(bucket: \"%s\") "
							+ "|> range(start: -%s) "
							+ "|> filter(fn: (r) => r._measurement == \"%s\") "
							+ "|> filter(fn: (r) => r.deviceId == \"%s\") "
							+ aggregateWindow
							+ "|> yield(name: \"mean\")",
					InfluxConstants.BUCKET_STORAGE, range, InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL, deviceId
			);
			default -> throw new IllegalArgumentException("Unsupported measurement type: " + type);
		};

		Map<String, Object> result = new HashMap<>();
		try {
			List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);

			List<String> labels = new ArrayList<>();
			Map<String, List<Double>> fieldValuesMap = new HashMap<>();

			for (FluxTable table : tables) {
				String fieldName = null;
				for (FluxRecord record : table.getRecords()) {
					// Werte sammeln
					Object value = record.getValueByKey("_value");
					if (value instanceof Number) {
						// Feldname speichern
						if (fieldName == null) {
							fieldName = (String) record.getValueByKey("_field");
							fieldValuesMap.putIfAbsent(fieldName, new ArrayList<>());
						}

						// Zeitstempel hinzufügen
						labels.add(record.getTime().toString());

						// Wert dem entsprechenden Feld hinzufügen
						fieldValuesMap.get(fieldName).add(((Number) value).doubleValue());
					} else {
						if (value != null) {
							logger.warn("Unerwarteter Datentyp für _value: {}", value);
						}
					}
				}
			}

			// Labels (x-Achse) hinzufügen
			result.put("labels", labels);

			// Datasets für jedes Feld erstellen (y-Achse)
			List<Map<String, Object>> datasets = new ArrayList<>();

			int colorIndex = 0;
			int fieldCount = fieldValuesMap.keySet().size();

			for (Map.Entry<String, List<Double>> entry : fieldValuesMap.entrySet()) {

				String borderColor = chartColorPicker.generateColor(colorIndex, fieldCount, 1.0);
				String backgroundColor = chartColorPicker.generateColor(colorIndex, fieldCount, 0.2);
				colorIndex++;

				datasets.add(
						Map.of(
								"label", entry.getKey(), // Feldname als Label
								"data", entry.getValue(), // Werte des Feldes
								"borderColor", borderColor,
								"backgroundColor", backgroundColor
						)
				);
			}
			result.put("datasets", datasets);

		} catch (Exception e) {
			logger.error("Fehler in InfluxDeviceService.getDeviceMeasurements(): {}", e.getMessage());
		}

		return result;
	}
}
