package com.energytracker.webclients;

import com.energytracker.security.TokenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

/**
 * @author André Heinen
 */
@Configuration
public class WebClientAuthenticationFilter {

	private final TokenService tokenService;

	public WebClientAuthenticationFilter(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	public ExchangeFilterFunction authenticationFilter() {
		return (request, next) -> next.exchange(request)
				.flatMap(response -> {
					if (response.statusCode().value() == 401) {

						tokenService.refreshAccessToken();

						ClientRequest updatedRequest = ClientRequest.from(request)
								.headers(headers -> headers.setBearerAuth(tokenService.getAccessToken()))
								.build();

						return next.exchange(updatedRequest);
					}
					return Mono.just(response);
				});
	}

}
