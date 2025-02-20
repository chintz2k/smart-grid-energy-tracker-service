package com.energytracker.influx;

import com.energytracker.exception.InvalidFluxQueryFilter;
import com.energytracker.exception.InvalidTimeFormatForInflux;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class InfluxServiceHelper {

	private static final Logger logger = LoggerFactory.getLogger(InfluxServiceHelper.class);

	private final InfluxDBClient influxDBClient;
	private final ChartColorPicker chartColorPicker;

	@Autowired
	public InfluxServiceHelper(InfluxDBClient influxDBClient, ChartColorPicker chartColorPicker) {
		this.influxDBClient = influxDBClient;
		this.chartColorPicker = chartColorPicker;
	}

	public String createInfluxQuery(
			@NotBlank @NotNull String bucket,
			String range,
			String measurementName,
			Map<String, String> filters,
			String aggregateWindowTime,
			String aggregateWindowType
	) {
		System.out.println("bucket: " + bucket + ", range: " + range + ", measurementName: " + measurementName + ", filters: " + filters + ", aggregateWindowTime: " + aggregateWindowTime + ", aggregateWindowType: " + aggregateWindowType);
		return "from(bucket: \"" + bucket + "\") " +
				addRange(range) +
				addMeasurement(measurementName) +
				addFilters(filters) +
				addAggregateWindow(aggregateWindowTime, aggregateWindowType) +
				"|> yield(name: \"mean\")";
	}

	public String addRange(String range) {
		if (range == null || range.isBlank()) {
			return "|> range(start: -5m) ";
		} else {
			range = ensureInfluxTimeUnit(range, "m");
			return "|> range(start: -" + range + ") ";
		}
	}

	public String addMeasurement(String measurement) {
		if (measurement == null || measurement.isBlank()) {
			return "";
		} else {
			return "|> filter(fn: (r) => r._measurement == \"" + measurement + "\") ";
		}
	}

	public String addFilters(Map<String, String> filters) {
		StringBuilder query = new StringBuilder();
		if (filters != null && !filters.isEmpty()) {
			for (Map.Entry<String, String> filter : filters.entrySet()) {
				if (filter.getKey() == null || filter.getKey().isBlank()) {
					throw new InvalidFluxQueryFilter("Filter-Key darf nicht leer sein.");
				}
				if (filter.getValue() == null || filter.getValue().isBlank()) {
					throw new InvalidFluxQueryFilter("Filter-Value darf nicht leer sein.");
				}
				query.append("|> filter(fn: (r) => r.").append(filter.getKey()).append(" == \"").append(filter.getValue()).append("\") ");
			}
			return query.toString();
		}
		return "";
	}

	public String addAggregateWindow(String aggregateWindowTime, String aggregateWindowType) {
		if (aggregateWindowTime == null || aggregateWindowTime.isBlank()) {
			return "";
		} else {
			aggregateWindowTime = ensureInfluxTimeUnit(aggregateWindowTime, "s");
			aggregateWindowType = ensureInfluxAggregationType(aggregateWindowType);
			return "|> aggregateWindow(every: " + aggregateWindowTime + ", fn: " + aggregateWindowType + ") ";
		}
	}

	public String ensureInfluxTimeUnit(String input, String defaultUnit) {
		// Prüfen, ob der String *nur* aus Zahlen besteht
		if (input.matches("^\\d+$")) {
			return input + defaultUnit; // Einheit anhängen (m für Range, s für Aggregation)
		}
		// Prüfen, ob der String bereits mit einer gültigen Influx-Zeiteinheit gegeben ist
		if (input.matches("^\\d+[smhdwy]$")) {
			return input; // Unverändert zurückgeben
		}
		// Als Fallback: Wenn ungültig, Standard-Zeit anhängen
		throw new InvalidTimeFormatForInflux(input + " ist keine gültige Zeiteinheit. Erwartet wird: dauer + m, s, h, d, w, M, y, n)");
	}

	public String ensureInfluxAggregationType(String type) {
		if (type.equals("mean") || type.equals("last") || type.equals("median")) {
			return type;
		} else {
			return "mean";
		}
	}

	public Map<String, Object> createMapForChartJsFromQuery(String query) {

		Map<String, Object> result = new HashMap<>();

		try {
			List<FluxTable> tables = influxDBClient.getQueryApi().query(query);

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
			logger.error("Fehler in {}: {}", this.getClass().getSimpleName(), e.getMessage());
		}

		return result;
	}
}
