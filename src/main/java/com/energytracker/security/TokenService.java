package com.energytracker.security;

import com.energytracker.dto.AuthenticationRequest;
import com.energytracker.dto.AuthenticationResponse;
import com.energytracker.dto.TokenRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

/**
 * @author André Heinen
 */
@Service
public class TokenService {

	private final RestTemplate restTemplate;
	private final JwtUtil jwtUtil;

	private String accessToken;
	private String refreshToken;

	@Autowired
	public TokenService(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate, JwtUtil jwtUtil) {
		this.restTemplate = restTemplate;
		this.jwtUtil = jwtUtil;
	}

	public synchronized String getAccessToken() {
		if (accessToken == null || isTokenExpired(accessToken)) {
			if (refreshToken != null) {
				refreshAccessToken();
			} else {
				authenticate();
			}
		}
		return accessToken;
	}

	private void authenticate() {
		AuthenticationRequest request = new AuthenticationRequest("john@example.com", "12345");

		AuthenticationResponse response = restTemplate.postForObject(
				"http://registry-service/login",
				request,
				AuthenticationResponse.class
		);

		if (response == null || response.accessToken() == null || response.refreshToken() == null) {
			throw new IllegalStateException("Authentication failed");
		}

		this.accessToken = response.accessToken();
		this.refreshToken = response.refreshToken();
	}

	public synchronized void refreshAccessToken() {
		TokenRequest request = new TokenRequest(this.refreshToken);

		AuthenticationResponse response = restTemplate.postForObject(
				"http://registry-service/refresh",
				request,
				AuthenticationResponse.class
		);

		if (response == null || response.accessToken() == null || response.refreshToken() == null) {
			throw new IllegalStateException("Token refresh failed");
		}

		this.accessToken = response.accessToken();
		this.refreshToken = response.refreshToken();
	}

	private boolean isTokenExpired(String token) {
		try {
			Date expirationDate = jwtUtil.getClaimsFromToken(token).getExpiration();

			Date bufferDate = new Date(expirationDate.getTime() - 2 * 60 * 1000); // 2 Minuten früher

			return bufferDate.before(new Date());
		} catch (Exception e) {
			return true;
		}
	}

}
