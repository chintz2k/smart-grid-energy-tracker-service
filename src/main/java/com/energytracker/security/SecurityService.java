package com.energytracker.security;

import com.energytracker.exception.exceptions.UnauthorizedAccessException;
import com.energytracker.influx.service.general.InfluxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author André Heinen
 */
@Service
public class SecurityService {

	private final JwtUtil jwtUtil;
	private final InfluxService influxService;

	@Autowired
	public SecurityService(JwtUtil jwtUtil, InfluxService influxService) {
		this.jwtUtil = jwtUtil;
		this.influxService = influxService;
	}

	public Long extractUserIdFromPrincipal(Object principal) {
		if (principal == null) {
			throw new UnauthorizedAccessException("No principal found, authentication is required");
		}

		if (principal instanceof Authentication) {
			if (principal instanceof UsernamePasswordAuthenticationToken) {
				String token = ((UsernamePasswordAuthenticationToken) principal).getCredentials().toString();
				return jwtUtil.extractUserId(token);
			}
		}

		throw new UnauthorizedAccessException("Invalid principal, cannot extract user ID");
	}

	public Long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new UnauthorizedAccessException("User is not authenticated");
		}

		if (authentication instanceof UsernamePasswordAuthenticationToken) {
			return jwtUtil.extractUserId(authentication.getCredentials().toString());
		}

		throw new UnauthorizedAccessException("Invalid user ID in authentication context");
	}

	public String getCurrentUserRole() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new UnauthorizedAccessException("User is not authenticated");
		}

		if (authentication instanceof UsernamePasswordAuthenticationToken) {
			String token = authentication.getCredentials().toString();
			return jwtUtil.extractRole(token);
		}

		throw new UnauthorizedAccessException("Invalid authentication context, cannot extract role");
	}

	public void checkIfDeviceBelongsToUserOrIsAdminOrIsSystem(Long deviceId) {
		Long ownerId = getCurrentUserId();
		if (deviceId != null) {
			if (!getCurrentUserRole().equals("ROLE_ADMIN") && !getCurrentUserRole().equals("ROLE_SYSTEM")) {
				if (!ownerId.equals(influxService.getDeviceOwnerId(deviceId))) {
					throw new UnauthorizedAccessException("Unauthorized access to consumer with ID " + deviceId + " for user with ID " + ownerId);
				}
			}
		}
	}

	public List<Long> getDevicesThatBelongToUserFromStringOrIsAdminOrIsSystem(String deviceId) {
		if (deviceId == null) {
			return null;
		}
		Long ownerId = getCurrentUserId();
		String[] splittedDeviceId = deviceId.split(",");
		List<Long> rawList = new ArrayList<>();
		for (String id : splittedDeviceId) {
			String trimmedId = id.trim();
			rawList.add(Long.parseLong(trimmedId));
		}
		List<Long> result = new ArrayList<>();
		for (Long id : rawList) {
			if (!getCurrentUserRole().equals("ROLE_ADMIN") && !getCurrentUserRole().equals("ROLE_SYSTEM")) {
				if (ownerId.equals(influxService.getDeviceOwnerId(id))) {
					result.add(id);
				}
			} else {
				result.add(id);
			}
		}
		if (rawList.isEmpty()) {
			throw new UnauthorizedAccessException("Unauthorized access to consumers with IDs " + rawList + " for user with ID " + ownerId);
		}
		return result;
	}

	public void checkIfUserIsOwnerOrIsAdminOrIsSystem(Long ownerId) {
		if (getCurrentUserRole().equals("ROLE_ADMIN") || getCurrentUserRole().equals("ROLE_SYSTEM")) {
			return;
		}
		if (!getCurrentUserId().equals(ownerId)) {
			throw new UnauthorizedAccessException("Unauthorized access for user with ID " + getCurrentUserId());
		}
	}

	public void checkIfUserIsAdminOrIsSystem() {
		if (!getCurrentUserRole().equals("ROLE_ADMIN") && !getCurrentUserRole().equals("ROLE_SYSTEM")) {
			throw new UnauthorizedAccessException("Unauthorized access for user with ID " + getCurrentUserId());
		}
	}
}
