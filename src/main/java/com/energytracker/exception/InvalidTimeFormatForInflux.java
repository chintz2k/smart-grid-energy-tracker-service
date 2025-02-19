package com.energytracker.exception;

/**
 * @author André Heinen
 */
public class InvalidTimeFormatForInflux extends RuntimeException {
	public InvalidTimeFormatForInflux(String message) {
		super(message);
	}
}
