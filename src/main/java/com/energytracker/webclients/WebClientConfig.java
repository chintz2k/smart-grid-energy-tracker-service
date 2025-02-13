package com.energytracker.webclients;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


/**
 * @author André Heinen
 */
@Configuration
public class WebClientConfig {

	@Bean(name = "loadBalancedWebClientBuilder")
	@LoadBalanced
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}
}
