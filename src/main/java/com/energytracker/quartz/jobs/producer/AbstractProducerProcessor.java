package com.energytracker.quartz.jobs.producer;

import com.energytracker.entity.BaseProducer;
import com.energytracker.influx.measurements.ProductionMeasurement;
import org.springframework.scheduling.annotation.Async;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author André Heinen
 */
public abstract class AbstractProducerProcessor<T extends BaseProducer> {

	protected abstract int getIntervalInSeconds();
	protected abstract void updateProducer(T producer);
	protected abstract void removeProducer(T producer);

	@Async("taskExecutor")
	public CompletableFuture<Void> processProducer(T producer, List<ProductionMeasurement> measurementsBatch) {
		try {
			Instant startTime = producer.getStartTime();
			Instant endTime = producer.getEndTime() != null ? producer.getEndTime() : Instant.now();
			Instant lastUpdate = producer.getLastUpdate() != null ? producer.getLastUpdate() : startTime;

			Instant current = lastUpdate;

			while (current.isBefore(endTime)) {
				Instant next = current.plusSeconds(getIntervalInSeconds());

				if (next.isAfter(endTime)) {
					next = endTime;
				}

				double intervalSeconds = Duration.between(current, next).getSeconds();
				double production = (producer.getPowerProduction() / 1000.0) * (intervalSeconds / 3600.0);

				ProductionMeasurement productionMeasurement = new ProductionMeasurement();
				productionMeasurement.setTimestamp(current);
				productionMeasurement.setDeviceId(producer.getDeviceId());
				productionMeasurement.setOwnerId(producer.getOwnerId());
				productionMeasurement.setProduction(production);
				productionMeasurement.setPowerType(producer.getPowerType());
				productionMeasurement.setRenewable(producer.isRenewable());

				measurementsBatch.add(productionMeasurement);

				current = next;
			}

			producer.setLastUpdate(current);
			updateProducer(producer);

			if (producer.getEndTime() != null && !current.isBefore(producer.getEndTime())) {
				removeProducer(producer);
			}

			return CompletableFuture.completedFuture(null);

		} catch (Exception e) {
			System.err.println("Fehler beim Verarbeiten von Hersteller " + producer.getDeviceId() + ": " + e.getMessage());
			e.printStackTrace();
			return CompletableFuture.failedFuture(e);
		}
	}
}
