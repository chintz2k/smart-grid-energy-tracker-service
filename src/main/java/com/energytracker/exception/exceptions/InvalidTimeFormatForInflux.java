package com.energytracker.exception.exceptions;

/**
 * @author André Heinen
 */
public class InvalidTimeFormatForInflux extends RuntimeException {
	public InvalidTimeFormatForInflux(String message) {
		super(message);
	}
}
