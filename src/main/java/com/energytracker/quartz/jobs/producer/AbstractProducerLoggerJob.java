package com.energytracker.quartz.jobs.producer;

import com.energytracker.dto.WeatherResponse;
import com.energytracker.entity.BaseProducer;
import com.energytracker.entity.CommercialProducer;
import com.energytracker.entity.Producer;
import com.energytracker.influx.InfluxDBService;
import com.energytracker.influx.measurements.ProductionMeasurement;
import com.energytracker.quartz.util.StorageHandler;
import com.energytracker.webclients.WeatherApiClient;
import org.jetbrains.annotations.Nullable;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author André Heinen
 */
@Component
public abstract class AbstractProducerLoggerJob<T extends BaseProducer> implements Job {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProducerLoggerJob.class);

	private static final int BATCH_SIZE = 500;

	private final InfluxDBService influxDBService;
	private final WeatherApiClient weatherApiClient;
	private final StorageHandler storageHandler;

	protected abstract List<T> getActiveProducers();
	protected abstract T getProducerById(Long id);
	protected abstract String getMeasurementName();
	protected abstract void updateAll(List<T> producerList);
	protected abstract void removeAll(List<T> producerList);
	protected abstract int getIntervalInSeconds();

	private double sunPowerModificator = 1.0;
	private double windPowerModificator = 1.0;

	@Autowired
	public AbstractProducerLoggerJob(InfluxDBService influxDBService, WeatherApiClient weatherApiClient, StorageHandler storageHandler) {
		this.influxDBService = influxDBService;
		this.weatherApiClient = weatherApiClient;
		this.storageHandler = storageHandler;
	}

	@Override
	@Async("taskExecutor")
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		List<T> activeProducers = getActiveProducers();
		List<ProductionMeasurement> measurementsBatch = Collections.synchronizedList(new ArrayList<>());
		List<T> updatedProducers = Collections.synchronizedList(new ArrayList<>());
		List<T> removedProducers = Collections.synchronizedList(new ArrayList<>());
		Map<Long, List<ProductionMeasurement>> ownerProductionData = new ConcurrentHashMap<>();

		WeatherResponse response = weatherApiClient.getWeather();
		if (response != null) {
			sunPowerModificator = response.getSolarPower();
			windPowerModificator = response.getWindPower();
		}

		processSyncedInBatches(activeProducers, measurementsBatch, updatedProducers, removedProducers, ownerProductionData);

		Map<Long, Double> totalProductionOfOwnerMap = getTotalProductionByOwnerAsMap(ownerProductionData);
		storageHandler.updateStorages(totalProductionOfOwnerMap, false);

		List<ProductionMeasurement> totalProductionByOwnerAndPowerTypeAndTimestamp = calculateTotalProductionByOwnerAndPowerTypeAndTimestamp(ownerProductionData);
		List<ProductionMeasurement> totalProductionByOwnerAndByTimestamp = calculateTotalProductionByOwnerAndTimestamp(ownerProductionData);
		List<ProductionMeasurement> totalProductionByPowerTypeAndTimestamp = calculateTotalProductionByPowerTypeAndTimestamp(ownerProductionData);
		List<ProductionMeasurement> totalProductionByTimestamp = calculateTotalProductionByTimestamp(ownerProductionData);
		updateDatabase(updatedProducers, removedProducers, measurementsBatch, totalProductionByOwnerAndPowerTypeAndTimestamp, totalProductionByOwnerAndByTimestamp, totalProductionByPowerTypeAndTimestamp, totalProductionByTimestamp);
	}

	private void processSynced(
			List<T> activeProducers,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers,
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {
		for (T producer : activeProducers) {
			Long id = getDeviceId(producer);
			if (id != null) {
				T latestProducer = getProducerById(id);
				if (latestProducer != null) {
					processProducer(latestProducer, measurementsBatch, updatedProducers, removedProducers, ownerProductionData);
				}
			}
		}
	}

	private void processSyncedInBatches(
			List<T> activeProducers,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers,
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {
		int totalProducers = activeProducers.size();

		for (int i = 0; i < totalProducers; i += BATCH_SIZE) {
			List<T> batch = activeProducers.subList(i, Math.min(i + BATCH_SIZE, totalProducers));

			for (T producer : batch) {
				Long id = getDeviceId(producer);
				if (id != null) {
					T latestProducer = getProducerById(id);
					if (latestProducer != null) {
						processProducer(latestProducer, measurementsBatch, updatedProducers, removedProducers, ownerProductionData);
					}
				}
			}
		}
	}

	private void updateDatabase(
			List<T> updatedProducers,
			List<T> removedProducers,
			List<ProductionMeasurement> measurementsBatch,
			List<ProductionMeasurement> totalProductionByOwnerAndPowerTypeAndTimestamp,
			List<ProductionMeasurement> totalProductionOfOwnerByTimestamp,
			List<ProductionMeasurement> totalProductionByPowerTypeAndTimestamp,
			List<ProductionMeasurement> totalProductionByTimestamp
	) {
		if (!updatedProducers.isEmpty()) {
			updateAll(updatedProducers);
		}

		if (!measurementsBatch.isEmpty()) {
			influxDBService.saveProductionMeasurements(measurementsBatch, getMeasurementName());
		}

		if (!totalProductionByOwnerAndPowerTypeAndTimestamp.isEmpty()) {
			influxDBService.saveProductionMeasurements(totalProductionByOwnerAndPowerTypeAndTimestamp, "production_owner");
		}

		if (!totalProductionOfOwnerByTimestamp.isEmpty()) {
			influxDBService.saveProductionMeasurements(totalProductionOfOwnerByTimestamp, "production_owner");
		}

		if (!totalProductionByPowerTypeAndTimestamp.isEmpty()) {
			influxDBService.saveProductionMeasurements(totalProductionByPowerTypeAndTimestamp, "production_total");
		}

		if (!totalProductionByTimestamp.isEmpty()) {
			influxDBService.saveProductionMeasurements(totalProductionByTimestamp, "production_total");
		}

		if (!removedProducers.isEmpty()) {
			removeAll(removedProducers);
		}
	}

	public void processProducer(
			T producer,
			List<ProductionMeasurement> measurementsBatch,
			List<T> updatedProducers,
			List<T> removedProducers,
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {
		try {
			// Initialisierung der Start- und Endzeiten der Messung
			Instant startTime = initializeStartTime(producer);
			Instant endTime = calculateEndTime(producer);

			// Prüfen, ob das Gerät gerade erst gestartet wurde
			boolean justStarted = producer.getLastUpdate() == null;
			// Prüfen, ob das Gerät schon beendet werden kann
			boolean canFinish = producer.getEndTime() != null;

			Instant prev = producer.getLastUpdate() == null ? producer.getStartTime() : producer.getLastUpdate();
			Instant intervalStart = processStartPeriod(producer, justStarted, startTime, measurementsBatch);

			// Falls das Gerät neu gestartet wird und direkt beendet werden kann
			if (justStarted && canFinish && !intervalStart.isBefore(endTime)) {
				processSingleInterval(producer, startTime, endTime, measurementsBatch, ownerProductionData);
				prev = endTime;
			}

			// Verbrauchsdaten für wiederholte Intervalle verarbeiten
			prev = processIntervals(producer, prev, intervalStart, endTime, measurementsBatch, ownerProductionData);

			// Letztes Intervall und Abschlussverarbeitung, falls nötig
			if (canFinish) {
				processEndPeriod(producer, prev, measurementsBatch, removedProducers, ownerProductionData);
			}

			// Speichern der letzten Aktualisierungszeit und Hinzufügen zur aktualisierten Liste
			producer.setLastUpdate(endTime);
			updatedProducers.add(producer);

		} catch (Exception e) {
			// Fehlerbehandlung mit detaillierter Ausgabe
			logger.error("Fehler in der processProducer-Methode für {}: {}", producer.getClass().getSimpleName(), e.getMessage());
		}
	}

	private Instant initializeStartTime(T producer) {
		if (producer.getLastUpdate() == null) {
			return producer.getStartTime();
		}
		return producer.getLastUpdate().plusSeconds(getIntervalInSeconds());
	}

	private Instant calculateEndTime(T producer) {
		if (producer.getEndTime() == null) {
			Instant currentTime = Instant.now();
			return currentTime.minusSeconds(currentTime.getEpochSecond() % getIntervalInSeconds())
					.truncatedTo(ChronoUnit.SECONDS);
		}
		return producer.getEndTime();
	}

	private Instant processStartPeriod(
			T producer,
			boolean justStarted,
			Instant startTime,
			List<ProductionMeasurement> measurementsBatch
	) {
		if (justStarted) {
			// Verbrauch initialisieren, wenn das Gerät gerade eingeschaltet wurde
			ProductionMeasurement zeroMeasurement = createZeroMeasurementAtStart(producer);
			measurementsBatch.add(zeroMeasurement);

			Instant alignedStart = startTime.minusSeconds(startTime.getEpochSecond() % getIntervalInSeconds())
					.truncatedTo(ChronoUnit.SECONDS)
					.plusSeconds(getIntervalInSeconds());
			return alignedStart;
		}
		return startTime;
	}

	private void processSingleInterval(
			T producer,
			Instant startTime,
			Instant endTime,
			List<ProductionMeasurement> measurementsBatch,
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {
		long elapsedTime = getElapsedTimeInMilliseconds(startTime, endTime);
		double production = getProduction(producer, elapsedTime);

		ProductionMeasurement measurement = createMeasurement(producer, endTime, production, ownerProductionData);
		measurementsBatch.add(measurement);
	}

	private Instant processIntervals(
			T producer,
			Instant prev,
			Instant intervalStart,
			Instant endTime,
			List<ProductionMeasurement> measurementsBatch,
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {
		while (!intervalStart.isAfter(endTime)) {
			long elapsedTime = getElapsedTimeInMilliseconds(prev, intervalStart);
			double production = getProduction(producer, elapsedTime);

			// Erfassen des Verbrauchs für das Intervall
			ProductionMeasurement measurement = createMeasurement(producer, intervalStart, production, ownerProductionData);
			measurementsBatch.add(measurement);

			prev = intervalStart;
			intervalStart = intervalStart.plusSeconds(getIntervalInSeconds());
		}
		return prev;
	}

	private void processEndPeriod(
			T producer,
			Instant prev,
			List<ProductionMeasurement> measurementsBatch,
			List<T> removedProducers,
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {
		if (!producer.getEndTime().equals(prev)) {
			// Letztes Intervall vor dem Ausschalten
			long lastIntervalElapsed = getElapsedTimeInMilliseconds(prev, producer.getEndTime());
			double production = getProduction(producer, lastIntervalElapsed);

			ProductionMeasurement lastMeasurement = createMeasurement(producer, producer.getEndTime(), production, ownerProductionData);
			measurementsBatch.add(lastMeasurement);
		}

		// Verbrauch nach dem Ende initialisieren und Verbraucher entfernen
		ProductionMeasurement zeroMeasurement = createZeroMeasurementAtEnd(producer);
		measurementsBatch.add(zeroMeasurement);
		removedProducers.add(producer);
	}

	private static <T extends BaseProducer> @Nullable Long getDeviceId(T producer) {
		Long id = null;
		if (producer instanceof Producer updatedProducer) {
			id = updatedProducer.getId();
		} else if (producer instanceof CommercialProducer updatedProducer) {
			id = updatedProducer.getId();
		}
		return id;
	}

	private double getProduction(T producer, long durationInMilliseconds) {
		if (producer.getPowerType().equals("Solar Power")) {
			return ((producer.getPowerProduction() / 1000.0) * (durationInMilliseconds / (1000.0 * 3600.0)) * sunPowerModificator);
		} else if (producer.getPowerType().equals("Wind Power")) {
			return ((producer.getPowerProduction() / 1000.0) * (durationInMilliseconds / (1000.0 * 3600.0)) * windPowerModificator);
		} else {
			return ((producer.getPowerProduction() / 1000.0) * (durationInMilliseconds / (1000.0 * 3600.0)));
		}
	}

	private long getElapsedTimeInMilliseconds(Instant start, Instant end) {
		return end.toEpochMilli() - start.toEpochMilli();
	}

	private ProductionMeasurement createZeroMeasurementAtStart(T producer) {
		ProductionMeasurement measurement = new ProductionMeasurement();
		Instant startTime = producer.getStartTime();
		startTime = startTime.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(startTime);
		measurement.setDeviceId(producer.getDeviceId());
		measurement.setOwnerId(producer.getOwnerId());
		measurement.setPowerType(producer.getPowerType());
		String renewable = producer.isRenewable() ? "true" : "false";
		measurement.setRenewable(renewable);
		measurement.setProduction(0.0);
		return measurement;
	}

	private ProductionMeasurement createZeroMeasurementAtEnd(T producer) {
		ProductionMeasurement measurement = new ProductionMeasurement();
		Instant endTime = producer.getEndTime().plusMillis(1);
		endTime = endTime.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(endTime);
		measurement.setDeviceId(producer.getDeviceId());
		measurement.setOwnerId(producer.getOwnerId());
		measurement.setPowerType(producer.getPowerType());
		String renewable = producer.isRenewable() ? "true" : "false";
		measurement.setRenewable(renewable);
		measurement.setProduction(0.0);
		return measurement;
	}

	private ProductionMeasurement createMeasurement(
			T producer,
			Instant time,
			double production,
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {
		ProductionMeasurement measurement = new ProductionMeasurement();
		Instant timestamp = time.truncatedTo(ChronoUnit.MILLIS);
		measurement.setTimestamp(timestamp);
		measurement.setDeviceId(producer.getDeviceId());
		measurement.setOwnerId(producer.getOwnerId());
		measurement.setPowerType(producer.getPowerType());
		String renewable = producer.isRenewable() ? "true" : "false";
		measurement.setRenewable(renewable);
		measurement.setProduction(production);
		updateOwnerProductionHistory(producer.getOwnerId(), measurement, ownerProductionData);
		return measurement;
	}

	private void updateOwnerProductionHistory(
			Long ownerId,
			ProductionMeasurement measurement,
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {
		ownerProductionData.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(measurement);
	}

	private Map<Long, Double> getTotalProductionByOwnerAsMap(Map<Long, List<ProductionMeasurement>> ownerProductionData) {
		// Erstelle eine neue Map, in der die Ergebnisse gespeichert werden.
		Map<Long, Double> totalProductionByOwner = new ConcurrentHashMap<>();

		// Iteriere über die bestehende Map und berechne die Gesamtproduktion pro Owner
		ownerProductionData.forEach((ownerId, measurements) -> {
			double totalProduction = measurements.stream()
					.mapToDouble(ProductionMeasurement::getProduction)
					.sum();
			totalProductionByOwner.put(ownerId, totalProduction);
		});

		return totalProductionByOwner;
	}

	private List<ProductionMeasurement> calculateTotalProductionByOwnerAndTimestamp(
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {

		// Liste, um die neuen Messungen zu sammeln
		List<ProductionMeasurement> totalMeasurements = new ArrayList<>();

		// Iteriere über jeden Owner und deren Produktionsdaten
		ownerProductionData.forEach((ownerId, measurements) -> {
			// Zusammenfassen der Produktion pro Zeitstempel
			Map<Instant, Double> productionByTimestamp = new ConcurrentHashMap<>();

			// Gehe durch die Messungen und summiere die Produktion pro Zeitstempel
			for (ProductionMeasurement measurement : measurements) {
				Instant timestamp = measurement.getTimestamp();
				double production = measurement.getProduction();

				// Füge Werte mit gleichem Zeitstempel zusammen
				productionByTimestamp.merge(timestamp, production, Double::sum);
			}

			// Erstelle für jeden Zeitstempel ein neues ProductionMeasurement mit der Gesamtproduktion
			productionByTimestamp.forEach((timestamp, totalProduction) -> {
				ProductionMeasurement totalMeasurement = new ProductionMeasurement();
				totalMeasurement.setTimestamp(timestamp);
				totalMeasurement.setDeviceId(null); // Kein spezifisches Device, da die Summe aller Devices umfasst ist
				totalMeasurement.setOwnerId(ownerId);
				totalMeasurement.setProduction(totalProduction);
				totalMeasurement.setPowerType("all");
				totalMeasurement.setRenewable("beides");

				totalMeasurements.add(totalMeasurement);
			});
		});

		return totalMeasurements;
	}

	private List<ProductionMeasurement> calculateTotalProductionByOwnerAndPowerTypeAndTimestamp(
			Map<Long, List<ProductionMeasurement>> ownerProductionData) {

		// Ergebnisliste für aggregierte Produktionsmessungen
		List<ProductionMeasurement> aggregatedMeasurements = new ArrayList<>();

		// Iteriere durch alle Owner und ihre Produktionsdaten
		for (Map.Entry<Long, List<ProductionMeasurement>> ownerEntry : ownerProductionData.entrySet()) {
			Long ownerId = ownerEntry.getKey();
			List<ProductionMeasurement> measurements = ownerEntry.getValue();

			// Map zur Aggregation: powerType -> (timestamp -> List<ProductionMeasurement>)
			Map<String, Map<Instant, List<ProductionMeasurement>>> organizedData = new HashMap<>();

			// Gruppiere die Messungen nach powerType und timestamp
			for (ProductionMeasurement measurement : measurements) {
				String powerType = measurement.getPowerType();
				Instant timestamp = measurement.getTimestamp();

				organizedData
						.computeIfAbsent(powerType, k -> new HashMap<>())
						.computeIfAbsent(timestamp, k -> new ArrayList<>())
						.add(measurement);
			}

			// Erstelle neue aggregierte `ProductionMeasurement`-Objekte
			for (Map.Entry<String, Map<Instant, List<ProductionMeasurement>>> powerTypeEntry : organizedData.entrySet()) {
				String powerType = powerTypeEntry.getKey();

				for (Map.Entry<Instant, List<ProductionMeasurement>> timestampEntry : powerTypeEntry.getValue().entrySet()) {
					Instant timestamp = timestampEntry.getKey();
					List<ProductionMeasurement> groupedMeasurements = timestampEntry.getValue();

					// Berechne die Gesamtproduktion
					double totalProduction = groupedMeasurements.stream()
							.mapToDouble(ProductionMeasurement::getProduction)
							.sum();

					// Bestimme den `renewable`-Wert
					Set<String> renewableValues = groupedMeasurements.stream()
							.map(ProductionMeasurement::getRenewable)
							.collect(Collectors.toSet());

					String renewable;
					if (renewableValues.size() == 1) {
						// Nur ein `renewable`-Wert vorhanden, entweder "true" oder "false"
						renewable = renewableValues.iterator().next();
					} else {
						// Gemischte `renewable`-Werte
						renewable = "mixed";
					}

					// Neues ProductionMeasurement mit aggregierten und ermittelten Werten erstellen
					ProductionMeasurement aggregatedMeasurement = new ProductionMeasurement();
					aggregatedMeasurement.setOwnerId(ownerId);
					aggregatedMeasurement.setTimestamp(timestamp);
					aggregatedMeasurement.setProduction(totalProduction);
					aggregatedMeasurement.setPowerType(powerType);
					aggregatedMeasurement.setRenewable(renewable); // Setzen des renewable-Wertes

					// Füge zur Ergebnisliste hinzu
					aggregatedMeasurements.add(aggregatedMeasurement);
				}
			}
		}

		return aggregatedMeasurements;
	}

	private List<ProductionMeasurement> calculateTotalProductionByTimestamp(
			Map<Long, List<ProductionMeasurement>> ownerProductionData
	) {

		// Liste, um die neuen Messungen zu sammeln
		List<ProductionMeasurement> totalMeasurments = new ArrayList<>();

		// Map zur Aggregation der Produktion je Timestamp
		Map<Instant, Double> productionByTimestamp = new ConcurrentHashMap<>();

		// Iteriere durch alle Owners und deren Produktionsdaten
		ownerProductionData.forEach((ownerId, measurements) -> {
			// Gehe durch jede Produktionsmessung des aktuellen Owners
			for (ProductionMeasurement measurement : measurements) {
				Instant timestamp = measurement.getTimestamp();
				double production = measurement.getProduction();

				// Summiere die Produktion für jeden Zeitstempel
				productionByTimestamp.merge(timestamp, production, Double::sum);
			}
		});

		// Erstelle für jeden Zeitstempel ein neues ProductionMeasurement und füge es der Liste hinzu
		productionByTimestamp.forEach((timestamp, totalProduction) -> {
			ProductionMeasurement totalMeasurement = new ProductionMeasurement();
			totalMeasurement.setTimestamp(timestamp);
			totalMeasurement.setDeviceId(null); // Kein spezifisches Device, da die Summe aller Devices umfasst ist
			totalMeasurement.setOwnerId(null); // Kein spezifischer Owner, da die Summe aller Owner umfasst ist
			totalMeasurement.setProduction(totalProduction);
			totalMeasurement.setPowerType("all");
			totalMeasurement.setRenewable("mixed");

			totalMeasurments.add(totalMeasurement);
		});

		return totalMeasurments;
	}

	public List<ProductionMeasurement> calculateTotalProductionByPowerTypeAndTimestamp(
			Map<Long, List<ProductionMeasurement>> ownerProductionData) {

		// Map zur Aggregation: powerType -> (timestamp -> List<ProductionMeasurement>)
		Map<String, Map<Instant, List<ProductionMeasurement>>> aggregatedData = new HashMap<>();

		// Iteriere durch alle Owner und ihre Produktionsdaten und mische sie zu einer allgemeinen Struktur
		for (List<ProductionMeasurement> measurements : ownerProductionData.values()) {
			for (ProductionMeasurement measurement : measurements) {
				String powerType = measurement.getPowerType();
				Instant timestamp = measurement.getTimestamp();

				aggregatedData
						.computeIfAbsent(powerType, k -> new HashMap<>())
						.computeIfAbsent(timestamp, k -> new ArrayList<>())
						.add(measurement);
			}
		}

		// Ergebnisliste für aggregierte Produktionsmessungen
		List<ProductionMeasurement> aggregatedMeasurements = new ArrayList<>();

		// Verarbeite aggregierte Daten und erstelle neue Measurements
		for (Map.Entry<String, Map<Instant, List<ProductionMeasurement>>> powerTypeEntry : aggregatedData.entrySet()) {
			String powerType = powerTypeEntry.getKey();

			for (Map.Entry<Instant, List<ProductionMeasurement>> timestampEntry : powerTypeEntry.getValue().entrySet()) {
				Instant timestamp = timestampEntry.getKey();
				List<ProductionMeasurement> groupedMeasurements = timestampEntry.getValue();

				// Berechne die Gesamtproduktion
				double totalProduction = groupedMeasurements.stream()
						.mapToDouble(ProductionMeasurement::getProduction)
						.sum();

				// Bestimme den `renewable`-Wert
				Set<String> renewableValues = groupedMeasurements.stream()
						.map(ProductionMeasurement::getRenewable)
						.collect(Collectors.toSet());

				String renewable;
				if (renewableValues.size() == 1) {
					// Nur ein `renewable`-Wert vorhanden, entweder "true" oder "false"
					renewable = renewableValues.iterator().next();
				} else {
					// Gemischte `renewable`-Werte
					renewable = "mixed";
				}

				// Neues ProductionMeasurement mit aggregierten Werten erstellen
				ProductionMeasurement aggregatedMeasurement = new ProductionMeasurement();
				aggregatedMeasurement.setTimestamp(timestamp);
				aggregatedMeasurement.setDeviceId(null);
				aggregatedMeasurement.setOwnerId(null);
				aggregatedMeasurement.setProduction(totalProduction);
				aggregatedMeasurement.setPowerType(powerType);
				aggregatedMeasurement.setRenewable(renewable);

				// Füge zur Ergebnisliste hinzu
				aggregatedMeasurements.add(aggregatedMeasurement);
			}
		}

		return aggregatedMeasurements;
	}
}
