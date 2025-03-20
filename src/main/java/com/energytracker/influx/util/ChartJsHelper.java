package com.energytracker.influx.util;

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

	public ChartJsHelper(InfluxDBClient influxDBClient, ChartColorPicker chartColorPicker) {
		this.influxDBClient = influxDBClient;
		this.chartColorPicker = chartColorPicker;
	}

	public Map<String, Object> createMapForChartJsFromQuery(String query, boolean fill) {

		Map<String, Object> result = new HashMap<>();

		try {
			List<FluxTable> tables = influxDBClient.getQueryApi().query(query);

			List<String> labels = new ArrayList<>(); // Labels für Zeitstempel
			List<Map<String, Object>> datasets = new ArrayList<>(); // Datenstruktur für die eigentlichen Daten

			for (FluxTable table : tables) {
				// Jede Tabelle entspricht einem field
				String outputString = "";
				if (!table.getRecords().isEmpty()) {
					if (Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_TOTAL)
							|| Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_OWNER)) {
						outputString = "Verbrauch";
					} else if (Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_TOTAL)
							|| Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_OWNER)) {
						String powerType = Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("powerType")).toString();
						outputString = getCleanPowerString(powerType);
					} else if (Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_TOTAL)
							|| Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_COMMERCIAL)
							|| Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_STORAGE_TOTAL_PRIVATE)
							|| Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_STORAGE_OWNER)) {
						if (Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("_field")).toString().equalsIgnoreCase("capacity")) {
							outputString = "Kapazität";
						} else if (Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("_field")).toString().equalsIgnoreCase("currentCharge")) {
							outputString = "Ladung";
						}
					} else if (Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_NET)) {
						if (Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("_field")).toString().equalsIgnoreCase("currentBalance")) {
							outputString = "Aktuelle Netzlast";
						} else if (Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("_field")).toString().equalsIgnoreCase("change")) {
							outputString = "Letzte Änderung";
						}
					} else if (Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_CONSUMPTION_DEVICE)) {
						outputString = "Gerät: " + Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("deviceId"));
					} else if (Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_PRODUCTION_DEVICE)) {
						String powerType = Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("powerType")).toString();
						outputString = "Gerät: " + Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("deviceId")) + " / " + getCleanPowerString(powerType);
					} else if (Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_STORAGE_COMMERCIAL)
							|| Objects.requireNonNull(table.getRecords().getFirst().getMeasurement()).equalsIgnoreCase(InfluxConstants.MEASUREMENT_NAME_STORAGE)) {
						if (Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("_field")).toString().equalsIgnoreCase("capacity")) {
							outputString = "Kapazität von " + Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("deviceId"));
						} else if (Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("_field")).toString().equalsIgnoreCase("currentCharge")) {
							outputString = "Ladung von " + Objects.requireNonNull(table.getRecords().getFirst().getValueByKey("deviceId"));
						}
					}
				}

				Map<String, List<Object>> fieldValuesMap = new HashMap<>();

				for (FluxRecord record : table.getRecords()) {
					String time = Objects.requireNonNull(record.getTime()).toString();
					if (!labels.contains(time)) {
						labels.add(time);
					}
					String field = (String) record.getValueByKey("_field");
					fieldValuesMap.putIfAbsent(field, new ArrayList<>());
					fieldValuesMap.get(field).add(record.getValueByKey("_value"));
				}

				// Für jedes Feld im Gerät eine eigene Kurve hinzufügen
				for (Map.Entry<String, List<Object>> fieldEntry : fieldValuesMap.entrySet()) {
					Map<String, Object> dataset = new HashMap<>();
					dataset.put("label", outputString);
					dataset.put("data", fieldEntry.getValue());
					dataset.put("borderColor", chartColorPicker.generateColor(datasets.size(), tables.size() * fieldValuesMap.size(), 1.0));
					dataset.put("backgroundColor", chartColorPicker.generateColor(datasets.size(), tables.size() * fieldValuesMap.size(), 0.2));
					dataset.put("fill", fill);
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

	private String getCleanPowerString(String powerType) {
		if (powerType == null || powerType.isBlank()) {
			return "Produktion - Unbekannt";
		}
		if (powerType.equalsIgnoreCase("Solar Power")) {
			return "Produktion - Solar";
		} else if (powerType.equalsIgnoreCase("Wind Power")) {
			return "Produktion - Wind";
		} else if (powerType.equalsIgnoreCase("Hydro Power")) {
			return "Produktion - Wasser";
		} else if (powerType.equalsIgnoreCase("Geothermal Power")) {
			return "Produktion - Geothermal";
		} else if (powerType.equalsIgnoreCase("Biomass Power")) {
			return "Produktion - Biomasse";
		} else if (powerType.equalsIgnoreCase("Coal Power")) {
			return "Produktion - Kohle";
		} else if (powerType.equalsIgnoreCase("Natural Gas Power")) {
			return "Produktion - Gas";
		} else if (powerType.equalsIgnoreCase("Oil Power")) {
			return "Produktion - Oil";
		} else if (powerType.equalsIgnoreCase("Nuclear Power")) {
			return "Produktion - Nuclear";
		} else if (powerType.equalsIgnoreCase("Alle Energiearten")) {
			return "Produktion - Alle";
		} else {
			return "Produktion - Unbekannt";
		}
	}
}
