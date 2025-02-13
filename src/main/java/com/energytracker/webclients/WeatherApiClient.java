package com.energytracker.webclients;

import com.energytracker.dto.WeatherResponse;
import com.energytracker.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author André Heinen
 */
@Service
public class WeatherApiClient {

	private final WebClient webClient;

	private final TokenService tokenService;

	@Autowired
	public WeatherApiClient(
			@Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder,
			WebClientAuthenticationFilter authenticationFilter,
			TokenService tokenService
	) {
		this.tokenService = tokenService;
		this.webClient = builder
				.filter(authenticationFilter.authenticationFilter())
				.baseUrl("http://weather-api")
				.build();
	}

	public WeatherResponse getWeather() {
		return webClient.get()
				.uri("/weather")
				.header("Authorization", "Bearer " + tokenService.getAccessToken())
				.retrieve()
				.bodyToMono(WeatherResponse.class)
				.block();
	}
}
