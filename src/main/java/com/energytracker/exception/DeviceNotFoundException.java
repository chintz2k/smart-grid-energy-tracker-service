package com.energytracker.exception;

/**
 * @author André Heinen
 */
public class DeviceNotFoundException extends RuntimeException {
	public DeviceNotFoundException(String message) {
		super(message);
	}
}
