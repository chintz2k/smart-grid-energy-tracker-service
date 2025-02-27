package com.energytracker.exception.exceptions;

/**
 * @author André Heinen
 */
public class InvalidInfluxAggregationType extends RuntimeException {
	public InvalidInfluxAggregationType(String message) {
		super(message);
	}
}
