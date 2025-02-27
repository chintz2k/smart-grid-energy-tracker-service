package com.energytracker.exception.exceptions;

/**
 * @author André Heinen
 */
public class DuplicateDeviceFoundException extends RuntimeException {
	public DuplicateDeviceFoundException(String message) {
		super(message);
	}
}
