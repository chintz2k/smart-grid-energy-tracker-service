package com.energytracker.security;

import com.energytracker.dto.AuthenticationRequest;
import com.energytracker.dto.AuthenticationResponse;
import com.energytracker.dto.TokenRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author André Heinen
 */
@Service
public class TokenService {

	private final WebClient webClient;

	private String accessToken;
	private String refreshToken;

	private final Object lock = new Object();

	@Autowired
	public TokenService(@Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder) {
		this.webClient = builder.baseUrl("http://registry-service").build();
	}

	public String getAccessToken() {
		if (accessToken == null) {
			authenticate();
		}
		return accessToken;
	}

	private void authenticate() {
		AuthenticationRequest request = new AuthenticationRequest("john@example.com", "12345");
		AuthenticationResponse response = webClient.post()
				.uri("/login")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(AuthenticationResponse.class)
				.block();

		assert response != null;
		this.accessToken = response.accessToken();
		this.refreshToken = response.refreshToken();
	}

	public void refreshAccessToken() {
		synchronized (lock) {
			TokenRequest request = new TokenRequest(this.refreshToken);
			AuthenticationResponse response = webClient.post()
					.uri("/refresh")
					.bodyValue(request)
					.retrieve()
					.bodyToMono(AuthenticationResponse.class)
					.block();

			assert response != null;
			this.accessToken = response.accessToken();
			this.refreshToken = response.refreshToken();
		}
	}
}
