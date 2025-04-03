package com.energytracker.webclients;

import com.energytracker.kafka.events.SmartTimeslotTrackerEvent;
import com.energytracker.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author André Heinen
 */
@Service
public class DeviceApiClient {

	private static final Logger logger = LoggerFactory.getLogger(DeviceApiClient.class);

	private final RestTemplate restTemplate;
	private final TokenService tokenService;

	public DeviceApiClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate, TokenService tokenService) {
		this.restTemplate = restTemplate;
		this.tokenService = tokenService;
	}

	public void setActiveByListAndNoSendEvent(Set<Long> idList, boolean active, String type) {
		if (!Objects.equals(type, "consumers") && !Objects.equals(type, "producers")) {
			throw new IllegalArgumentException("Invalid device type");
		}
		String url = "http://home-builder-service/api/" + type + "/setActiveByListNoSendEvent?active=" + active;
		String accessToken = tokenService.getAccessToken();

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.set("Content-Type", "application/json");

		HttpEntity<Set<Long>> requestEntity = new HttpEntity<>(idList, headers);

		try {
			restTemplate.exchange(
					url,
					HttpMethod.PUT,
					requestEntity,
					new ParameterizedTypeReference<Map<String, String>>() {
					}
			);
		} catch (HttpClientErrorException e) {
			logger.error("SetActive - Client-Fehler: {} - {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), url);
		} catch (HttpServerErrorException e) {
			logger.error("SetActive - Server-Fehler: {} - {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), url);
		} catch (ResourceAccessException e) {
			logger.error("SetActive - Netzwerkproblem oder API nicht erreichbar: {}", e.getMessage());
		} catch (RestClientException e) {
			logger.error("SetActive - Fehler bei der Anfrage: {}", e.getMessage());
		} catch (Exception e) {
			logger.error("SetActive - Ein unerwarteter Fehler ist aufgetreten: {}", e.getMessage());
		}
	}

	public void updateTimeslotStatus(SmartTimeslotTrackerEvent event) {
		String url = "http://home-builder-service/api/smartconsumertimeslots/sysupdate";
		String accessToken = tokenService.getAccessToken();

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.set("Content-Type", "application/json");

		HttpEntity<SmartTimeslotTrackerEvent> requestEntity = new HttpEntity<>(event, headers);

		try {
			restTemplate.exchange(
					url,
					HttpMethod.PUT,
					requestEntity,
					new ParameterizedTypeReference<Map<String, String>>() {
					}
			);
		} catch (HttpClientErrorException e) {
			logger.error("UpdateTimeslotStatus - Client-Fehler: {} - {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), url);
		} catch (HttpServerErrorException e) {
			logger.error("UpdateTimeslotStatus - Server-Fehler: {} - {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), url);
		} catch (ResourceAccessException e) {
			logger.error("UpdateTimeslotStatus - Netzwerkproblem oder API nicht erreichbar: {}", e.getMessage());
		} catch (RestClientException e) {
			logger.error("UpdateTimeslotStatus - Fehler bei der Anfrage: {}", e.getMessage());
		} catch (Exception e) {
			logger.error("UpdateTimeslotStatus - Ein unerwarteter Fehler ist aufgetreten: {}", e.getMessage());
		}
	}
}
