package com.energytracker.quartz.util;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author André Heinen
 */
public class MeasurementLogger {

	private final Map<Instant, String> map = new ConcurrentHashMap<>();

	public MeasurementLogger() {

	}

	public void addEntry(Instant timestamp, String message) {
		map.put(timestamp, message);
	}

	public String getAllEntries() {
		if (!map.isEmpty()) {
			StringBuilder sortedNews = new StringBuilder();

			map.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(entry -> sortedNews.append(entry.getKey())
							.append(" - ")
							.append(entry.getValue())
							.append("\n")
					);

			if (!sortedNews.isEmpty() && sortedNews.charAt(sortedNews.length() - 1) == '\n') {
				sortedNews.setLength(sortedNews.length() - 1);
			}

			return sortedNews.toString();
		}
		return null;
	}

	public void printAllEntries() {
		if (getAllEntries() != null) {
			System.out.println(getAllEntries());
		}
	}
}
