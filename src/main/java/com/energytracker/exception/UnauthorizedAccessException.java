package com.energytracker.exception;

/**
 * @author André Heinen
 */
public class UnauthorizedAccessException extends RuntimeException {
	public UnauthorizedAccessException(String message) {
		super(message);
	}
}
