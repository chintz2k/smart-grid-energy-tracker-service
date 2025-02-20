package com.energytracker.exception;

/**
 * @author André Heinen
 */
public class InvalidInfluxAggregationType extends RuntimeException {
	public InvalidInfluxAggregationType(String message) {
		super(message);
	}
}
