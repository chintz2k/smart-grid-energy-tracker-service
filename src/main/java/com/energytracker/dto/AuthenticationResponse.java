package com.energytracker.dto;

/**
 * @author André Heinen
 */
public record AuthenticationResponse(String accessToken, String refreshToken) {}
