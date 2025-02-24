package com.energytracker.influx.util;

import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author André Heinen
 */
@Configuration
public class InfluxConstants {

	public static final String ORG_NAME = "chintz_de";

	public static final String BUCKET_CONSUMPTION = "energy_tracker";
	public static final String BUCKET_PRODUCTION = "energy_tracker";
	public static final String BUCKET_STORAGE = "energy_tracker";
	public static final String BUCKET_NET = "energy_tracker";

	public static final String MEASUREMENT_NAME_CONSUMPTION_DEVICE = "consumption_device";
	public static final String MEASUREMENT_NAME_PRODUCTION_DEVICE = "production_device";

	public static final String MEASUREMENT_NAME_CONSUMPTION_OWNER = "consumption_owner";
	public static final String MEASUREMENT_NAME_PRODUCTION_OWNER = "production_owner";

	public static final String MEASUREMENT_NAME_CONSUMPTION_TOTAL = "consumption_total";
	public static final String MEASUREMENT_NAME_PRODUCTION_TOTAL = "production_total";

	public static final String MEASUREMENT_NAME_STORAGE = "storages";
	public static final String MEASUREMENT_NAME_STORAGE_COMMERCIAL = "storages_commercial";

	public static final String MEASUREMENT_NAME_STORAGE_OWNER = "storages_owner";

	public static final String MEASUREMENT_NAME_STORAGE_TOTAL_PRIVATE = "storages_private_total";
	public static final String MEASUREMENT_NAME_STORAGE_TOTAL_COMMERCIAL = "storages_commercial_total";
	public static final String MEASUREMENT_NAME_STORAGE_TOTAL_TOTAL = "storages_total_total";

	public static final String MEASUREMENT_NAME_NET = "net_balance";

	public static final List<String> POWER_TYPES = List.of("Solar Power", "Wind Power", "Hydro Power", "Geothermal Power", "Biomass Power", "Coal Power", "Natural Gas Power", "Oil Power", "Nuclear Power");
}
