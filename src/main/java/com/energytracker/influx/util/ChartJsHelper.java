package com.energytracker.influx.util;

import com.energytracker.security.SecurityService;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author André Heinen
 */
@Service
public class ChartJsHelper {

	private static final Logger logger = LoggerFactory.getLogger(ChartJsHelper.class);

	private final InfluxDBClient influxDBClient;
	private final ChartColorPicker chartColorPicker;
	private final SecurityService securityService;

	public ChartJsHelper(InfluxDBClient influxDBClient, ChartColorPicker chartColorPicker, SecurityService securityService) {
		this.influxDBClient = influxDBClient;
		this.chartColorPicker = chartColorPicker;
		this.securityService = securityService;
	}

	public Map<String, Object> createMapForChartJsFromQuery(String query, boolean fill) {

		Map<String, Object> result = new HashMap<>();

		try {
			List<FluxTable> tables = influxDBClient.getQueryApi().query(query);

			List<String> labels = new ArrayList<>(); // Labels für Zeitstempel
			List<Map<String, Object>> datasets = new ArrayList<>(); // Datenstruktur für die DataSets

			for (FluxTable table : tables) {
				// Jede Tabelle entspricht einem Gerät
				String deviceId = table.getRecords().isEmpty() ? "" : (String) table.getRecords().getFirst().getValueByKey("deviceId");
				String ownerId = table.getRecords().isEmpty() ? "" : (String) table.getRecords().getFirst().getValueByKey("ownerId");
				String powerType = table.getRecords().isEmpty() ? "" : (String) table.getRecords().getFirst().getValueByKey("powerType");
				boolean currentBalance = !table.getRecords().isEmpty() && table.getRecords().getFirst().getValues().toString().contains("currentBalance");
				boolean change = !table.getRecords().isEmpty() && table.getRecords().getFirst().getValues().toString().contains("change");
				String outputString = "";
				if (deviceId == null || deviceId.isBlank()) {
					outputString += "Alle Geräte";
				} else {
					outputString += "Gerät: " + deviceId;
				}
				if (powerType == null) {
					outputString += "";
				} else {
					outputString += " - " + powerType;
				}
				if (ownerId == null || ownerId.isBlank()) {
					outputString += " - Alle Besitzer";
				} else {
					if (securityService.getCurrentUserRole().equals("ROLE_ADMIN") || securityService.getCurrentUserRole().equals("ROLE_SYSTEM")) {
						outputString += " - Besitzer: " + ownerId;
					}
				}
				if (currentBalance || change) {
					outputString = "";
				}
				if (!outputString.isBlank()) {
					outputString += " - ";
				}

				Map<String, List<Object>> fieldValuesMap = new HashMap<>();

				for (FluxRecord record : table.getRecords()) {
					String time = Objects.requireNonNull(record.getTime()).toString();
					if (!labels.contains(time)) {
						labels.add(time); // Eindeutige Zeitstempel hinzufügen
					}

					// Die Felder des Geräts identifizieren
					String field = (String) record.getValueByKey("_field");
					fieldValuesMap.putIfAbsent(field, new ArrayList<>());
					fieldValuesMap.get(field).add(record.getValueByKey("_value"));
				}

				// Für jedes Feld im Gerät eine eigene Serie (Kurve) hinzufügen
				for (Map.Entry<String, List<Object>> fieldEntry : fieldValuesMap.entrySet()) {
					Map<String, Object> dataset = new HashMap<>();
					dataset.put("label", outputString + fieldEntry.getKey()); // Kombiniertes Label aus Gerät und Feld
					dataset.put("data", fieldEntry.getValue()); // Werte für das Feld
					dataset.put("borderColor", chartColorPicker.generateColor(datasets.size(), tables.size() * fieldValuesMap.size(), 1.0)); // Linienfarbe
					dataset.put("backgroundColor", chartColorPicker.generateColor(datasets.size(), tables.size() * fieldValuesMap.size(), 0.2)); // Hintergrundfarbe
					dataset.put("fill", fill); // Keine Füllung unter der Linie
					datasets.add(dataset);
				}
			}

			if (!labels.isEmpty() && !datasets.isEmpty()) {
				result.put("labels", labels);
				result.put("datasets", datasets);
			}

		} catch (Exception e) {
			logger.error("Fehler in {}: {}", this.getClass().getSimpleName(), e.getMessage());
		}
		return result;
	}
}
