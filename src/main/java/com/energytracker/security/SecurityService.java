package com.energytracker.security;

import com.energytracker.exception.UnauthorizedAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author André Heinen
 */
@Service
public class SecurityService {

	private final JwtUtil jwtUtil;

	@Autowired
	public SecurityService(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
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
}
