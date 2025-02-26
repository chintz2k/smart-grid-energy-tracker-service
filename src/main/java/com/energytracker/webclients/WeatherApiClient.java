package com.energytracker.webclients;

import com.energytracker.dto.WeatherResponse;
import com.energytracker.security.TokenService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author André Heinen
 */
@Service
public class WeatherApiClient {

	private final RestTemplate restTemplate;
	private final TokenService tokenService;

	public WeatherApiClient(RestTemplate restTemplate, TokenService tokenService) {
		this.restTemplate = restTemplate;
		this.tokenService = tokenService;
	}

	// Abrufen von Wetterdaten über Registry-Service (Eureka, Load Balancer)
	public WeatherResponse getWeather() {
		String url = "http://weather-api/weather"; // Der Service-Name, nicht die IP/Port
		String accessToken = tokenService.getAccessToken();

		// Authorization Header mit dem Token als Bearer setzen
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		// HttpEntity mit Header erstellen
		HttpEntity<?> entity = new HttpEntity<>(headers);

		// Anfrage mit restTemplate und Authorization Header senden
		ResponseEntity<WeatherResponse> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				entity,
				WeatherResponse.class
		);

		return response.getBody();
	}

}
