package com.energytracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

/**
 * @author André Heinen
 */
@Configuration
public class DisableDefaultUser {

	@Bean
	public UserDetailsManager userDetailsService() {
		// Keine Standardbenutzer mehr
		return new InMemoryUserDetailsManager();
	}
}
