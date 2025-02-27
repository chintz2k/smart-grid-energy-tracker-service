package com.energytracker.entity.devices;

import com.energytracker.entity.devices.bases.BaseProducer;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author André Heinen
 */
@Entity
public class CommercialProducer extends BaseProducer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	public CommercialProducer() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
