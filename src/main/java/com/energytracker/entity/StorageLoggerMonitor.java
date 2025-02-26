package com.energytracker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * @author André Heinen
 */
@Entity
public class StorageLoggerMonitor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Instant timestamp;
	private long privateQueryTime;
	private long commercialQueryTime;
	private long privateDatabaseUpdateTime;
	private long commercialDatabaseUpdateTime;
	private int privateStoragesCount;
	private int commercialStoragesCount;
	private long privateStoragesTime;
	private long commercialStoragesTime;
	private long totalStoragesUpdateDatabaseTime;
	private int overallCount;
	private long overallTime;

	public StorageLoggerMonitor() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public long getPrivateQueryTime() {
		return privateQueryTime;
	}

	public void setPrivateQueryTime(long privateQueryTime) {
		this.privateQueryTime = privateQueryTime;
	}

	public long getCommercialQueryTime() {
		return commercialQueryTime;
	}

	public void setCommercialQueryTime(long commercialQueryTime) {
		this.commercialQueryTime = commercialQueryTime;
	}

	public long getPrivateDatabaseUpdateTime() {
		return privateDatabaseUpdateTime;
	}

	public void setPrivateDatabaseUpdateTime(long privateDatabaseUpdateTime) {
		this.privateDatabaseUpdateTime = privateDatabaseUpdateTime;
	}

	public long getCommercialDatabaseUpdateTime() {
		return commercialDatabaseUpdateTime;
	}

	public void setCommercialDatabaseUpdateTime(long commercialDatabaseUpdateTime) {
		this.commercialDatabaseUpdateTime = commercialDatabaseUpdateTime;
	}

	public int getPrivateStoragesCount() {
		return privateStoragesCount;
	}

	public void setPrivateStoragesCount(int privateStoragesCount) {
		this.privateStoragesCount = privateStoragesCount;
	}

	public int getCommercialStoragesCount() {
		return commercialStoragesCount;
	}

	public void setCommercialStoragesCount(int commercialStoragesCount) {
		this.commercialStoragesCount = commercialStoragesCount;
	}

	public long getPrivateStoragesTime() {
		return privateStoragesTime;
	}

	public void setPrivateStoragesTime(long privateStoragesTime) {
		this.privateStoragesTime = privateStoragesTime;
	}

	public long getCommercialStoragesTime() {
		return commercialStoragesTime;
	}

	public void setCommercialStoragesTime(long commercialStoragesTime) {
		this.commercialStoragesTime = commercialStoragesTime;
	}

	public long getTotalStoragesUpdateDatabaseTime() {
		return totalStoragesUpdateDatabaseTime;
	}

	public void setTotalStoragesUpdateDatabaseTime(long totalStoragesUpdateDatabaseTime) {
		this.totalStoragesUpdateDatabaseTime = totalStoragesUpdateDatabaseTime;
	}

	public int getOverallCount() {
		return overallCount;
	}

	public void setOverallCount(int overallCount) {
		this.overallCount = overallCount;
	}

	public long getOverallTime() {
		return overallTime;
	}

	public void setOverallTime(long overallTime) {
		this.overallTime = overallTime;
	}
}
