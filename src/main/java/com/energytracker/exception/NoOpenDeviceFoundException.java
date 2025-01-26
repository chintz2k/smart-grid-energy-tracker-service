package com.energytracker.exception;

/**
 * @author André Heinen
 */
public class NoOpenDeviceFoundException extends RuntimeException {
	public NoOpenDeviceFoundException(String message) {
		super(message);
	}
}
