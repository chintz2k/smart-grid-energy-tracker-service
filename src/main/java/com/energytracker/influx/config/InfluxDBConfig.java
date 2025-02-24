package com.energytracker.influx.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @author André Heinen
 */
@Configuration
public class InfluxDBConfig {

	@Value("${influx.url}")
	private String url;

	@Value("${influx.token}")
	private String token;

	@Value("${influx.org}")
	private String org;

	public InfluxDBConfig() {

	}

	@Bean
	public InfluxDBClient influxDBClient() {
		String decodedToken = URLDecoder.decode(token, StandardCharsets.UTF_8);
		return InfluxDBClientFactory.create(url, decodedToken.toCharArray(), org);
	}
}
