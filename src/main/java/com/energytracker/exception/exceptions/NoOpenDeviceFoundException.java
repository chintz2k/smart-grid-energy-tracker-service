package com.energytracker.exception.exceptions;

/**
 * @author André Heinen
 */
public class NoOpenDeviceFoundException extends RuntimeException {
	public NoOpenDeviceFoundException(String message) {
		super(message);
	}
}
