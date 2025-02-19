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
public class InfluxNetBalanceService {

	private static final Logger logger = LoggerFactory.getLogger(InfluxNetBalanceService.class);

	private final InfluxDBClient influxDBClient;
	private final SecurityService securityService;
	private final ChartColorPicker chartColorPicker;

	@Autowired
	public InfluxNetBalanceService(InfluxDBClient influxDBClient, SecurityService securityService, ChartColorPicker chartColorPicker) {
		this.influxDBClient = influxDBClient;
		this.securityService = securityService;
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

	public Map<String, Object> getNetBalanceMeasurementsForChartJs(String range, String aggregation) {
		if (!securityService.getCurrentUserRole().equals("ROLE_ADMIN") && !securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
			throw new UnauthorizedAccessException("Unauthorized access to net balance data for user with ID " + securityService.getCurrentUserId());
		}

		range = ensureTimeUnit(range, "m");

		String aggregateWindow;
		if (aggregation == null || aggregation.isBlank() || aggregation.equals("none") || aggregation.equals("false") || aggregation.equals("no")) {
			aggregateWindow = "";
		} else {
			aggregation = ensureTimeUnit(aggregation, "s");
			aggregateWindow = "|> aggregateWindow(every: " + aggregation + ", fn: mean) ";
		}

		String fluxQuery = String.format(
				"from(bucket: \"%s\") "
						+ "|> range(start: -%s) "
						+ "|> filter(fn: (r) => r._measurement == \"%s\") "
						+ aggregateWindow
						+ "|> yield(name: \"mean\")",
				InfluxConstants.BUCKET_NET, range, InfluxConstants.MEASUREMENT_NAME_NET
		);

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
			logger.error("Fehler in InfluxOwnerService.getOwnerMeasurements(): {}", e.getMessage());
		}

		return result;
	}

}
