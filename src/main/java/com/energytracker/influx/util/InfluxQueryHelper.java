package com.energytracker.influx.util;

import com.energytracker.exception.exceptions.InvalidTimeFormatForInflux;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxTable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author André Heinen
 */
@Service
public class InfluxQueryHelper {

	private final InfluxDBClient influxDBClient;

	@Autowired
	public InfluxQueryHelper(InfluxDBClient influxDBClient) {
		this.influxDBClient = influxDBClient;
	}

	public String createInfluxQuery(
			@NotBlank @NotNull String bucket,
			String range,
			String start,
			String end,
			List<String> measurementNames,
			List<Long> owners,
			List<Long> devices,
			Map<String, String> filters,
			String aggregateWindowTime,
			String aggregateWindowType
	) {
		return "from(bucket: \"" + bucket + "\") " +
				addRange(range, start, end) +
				addMeasurement(measurementNames) +
				addOwners(owners) +
				addDevices(devices) +
				addFilters(filters) +
				addAggregateWindow(aggregateWindowTime, aggregateWindowType) +
				"|> yield(name: \"mean\")";
	}

	public List<FluxTable> executeFluxQuery(String query) {
		try {
			return influxDBClient.getQueryApi().query(query);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String addRange(String range, String start, String end) {
		if (start != null && end != null) {
			return ensureInfluxTimeInterval(start, end);
		} else if (range != null) {
			range = ensureInfluxTimeUnit(range, "m");
			return "|> range(start: -" + range + ") ";
		} else {
			return "|> range(start: -5m) ";
		}
	}

	private String addMeasurement(List<String> measurements) {
		if (measurements != null && !measurements.isEmpty()) {
			if (measurements.size() > 1) {
				StringBuilder filterQuery = new StringBuilder("|> filter(fn: (r) => ");
				for (String measurement : measurements) {
					filterQuery
							.append("r._measurement == \"")
							.append(measurement)
							.append("\" or ");
				}
				filterQuery.delete(filterQuery.length() - 4, filterQuery.length());
				filterQuery.append(")");
				return filterQuery.toString();
			} else {
				return "|> filter(fn: (r) => r._measurement == \"" + measurements.getFirst() + "\")";
			}
		}
		throw new RuntimeException("Ungültiges Format für Influx Measurements");
	}

	private String addOwners(List<Long> ownerIds) {
		String ownerString;
		if (ownerIds == null || ownerIds.isEmpty()) {
			return "";
		}
		if (ownerIds.size() == 1) {
			ownerString = "|> filter(fn: (r) => r[\"ownerId\"] == \"" + ownerIds.getFirst() + "\")";
		} else {
			StringBuilder multipleOwners = new StringBuilder("|> filter(fn: (r) => ");
			for (Long id : ownerIds) {
				multipleOwners
						.append("r[\"ownerId\"] == \"")
						.append(id)
						.append("\" or ");
			}
			multipleOwners.delete(multipleOwners.length() - 4, multipleOwners.length());
			multipleOwners.append(")");
			ownerString = multipleOwners.toString();
		}
		return ownerString;
	}

	private String addDevices(List<Long> deviceIds) {
		String deviceString;
		if (deviceIds == null || deviceIds.isEmpty()) {
			return "";
		}
		if (deviceIds.size() == 1) {
			deviceString = "|> filter(fn: (r) => r[\"deviceId\"] == \"" + deviceIds.getFirst() + "\")";
		} else {
			StringBuilder multipleDevices = new StringBuilder("|> filter(fn: (r) => ");
			for (Long id : deviceIds) {
				multipleDevices
						.append("r[\"deviceId\"] == \"")
						.append(id)
						.append("\" or ");
			}
			multipleDevices.delete(multipleDevices.length() - 4, multipleDevices.length());
			multipleDevices.append(")");
			deviceString = multipleDevices.toString();
		}
		return deviceString;
	}

	private String addFilters(Map<String, String> filters) {
		StringBuilder filterQuery = new StringBuilder("|> filter(fn: (r) => ");

		List<String> conditions = new ArrayList<>();

		if (filters != null && !filters.isEmpty()) {
			for (Map.Entry<String, String> entry : filters.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();

				// Prüfen, ob der Wert kommaseparierte Einträge enthält
				if (value.contains(",")) {
					// Auseinandernehmen der Werte zu einer Liste
					String[] values = value.split(",");
					List<String> orConditions = new ArrayList<>();

					for (String v : values) {
						// Leerräume vor/nach jedem Wert entfernen
						String trimmedValue = v.trim();
						orConditions.add("r[\"" + key + "\"] == \"" + trimmedValue + "\"");
					}

					// Die Bedingungen mit "or" verbinden und als Gruppe hinzufügen
					conditions.add("(" + String.join(" or ", orConditions) + ")");

				} else {
					// Einzelner Wert: Standardcase
					// Leerräume vor/nach dem Wert entfernen
					String trimmedValue = value.trim();
					conditions.add("r[\"" + key + "\"] == \"" + trimmedValue + "\"");
				}
			}

			// Alle Bedingungen mit "and" verbinden
			filterQuery.append(String.join(" and ", conditions));
			filterQuery.append(")");

			return filterQuery.toString();
		} else {
			return "";
		}
	}

	private String addAggregateWindow(String aggregateWindowTime, String aggregateWindowType) {
		if (aggregateWindowTime == null || aggregateWindowTime.isBlank()) {
			return "";
		} else {
			aggregateWindowTime = ensureInfluxTimeUnit(aggregateWindowTime, "s");
			aggregateWindowType = ensureInfluxAggregationType(aggregateWindowType);
			return "|> aggregateWindow(every: " + aggregateWindowTime + ", fn: " + aggregateWindowType + ") ";
		}
	}

	private String ensureInfluxTimeUnit(String input, String defaultUnit) {
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

	private String ensureInfluxAggregationType(String type) {
		if (type.equals("mean") || type.equals("last") || type.equals("median")) {
			return type;
		} else {
			return "mean";
		}
	}

	private String ensureInfluxTimeInterval(String start, String end) {
		if (start == null || start.isBlank() || end == null || end.isBlank()) {
			throw new InvalidTimeFormatForInflux("Both 'start' and 'end' must be provided and non-empty.");
		}

		// Überprüfung auf ein gültiges ISO 8601-Format für Zeitangaben
		if (!start.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z") ||
				!end.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
			throw new InvalidTimeFormatForInflux(
					"Both 'start' and 'end' must be in ISO 8601 format (e.g., '2023-01-01T00:00:00Z').");
		}

		// Validierung, dass start vor end liegt
		if (start.compareTo(end) >= 0) {
			throw new InvalidTimeFormatForInflux("'start' must be earlier than 'end'.");
		}

		return String.format("|> range(start: %s, stop: %s)", start, end);
	}
}
