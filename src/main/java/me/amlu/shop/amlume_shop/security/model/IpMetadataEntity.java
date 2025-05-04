/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

import jakarta.persistence.*;
import me.amlu.shop.amlume_shop.model.BaseEntity;

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ip_metadata", indexes = {
        @Index(name = "idx_ip_address", columnList = "ip_address"),
        @Index(name = "idx_suspicious_count", columnList = "suspicious_count"),
        @Index(name = "idx_last_seen_at", columnList = "last_seen_at")
})
@Cacheable
// BaseEntity likely implements Serializable, so this class is Serializable.
public class IpMetadataEntity extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false, unique = true)
    private String ipAddress;

    @Column(name = "suspicious_count", nullable = false)
    private int suspiciousCount = 0;

    @Column(name = "last_geolocation", nullable = false)
    private String lastGeolocation;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    // --- ADD THIS FIELD DECLARATION ---
    @Column(name = "last_ttl") // Map to a database column
    private int lastTtl;
    // --- END ADDED FIELD ---

    // FIX: Ensure the List implementation used (ArrayList) is Serializable.
    // String is Serializable, so List<String> should be fine if ArrayList is used.
    @ElementCollection
    @CollectionTable(name = "ip_previous_geolocations")
    // Ensure the List implementation is Serializable (ArrayList is)
    private List<String> previousGeolocations = new ArrayList<>();

    // FIX: Ensure GeoLocationEntry implements Serializable.
    // Assuming GeoLocationEntry is an @Embeddable or another @Entity.
    // If it's an @Embeddable, it needs to implement Serializable.
    @ElementCollection
    @CollectionTable(name = "ip_geo_history")
    // Ensure the List implementation is Serializable (ArrayList is)
    // AND GeoLocationEntry itself must implement Serializable
    private List<GeoLocationEntry> geoHistory = new ArrayList<>();

    // FIX: Ensure the List implementation used (ArrayList) is Serializable.
    // Integer is Serializable, so List<Integer> should be fine if ArrayList is used.
    @ElementCollection
    @CollectionTable(name = "ip_ttl_history")
    // Ensure the List implementation is Serializable (ArrayList is)
    private List<Integer> ttlHistory = new ArrayList<>();

    public IpMetadataEntity() {
    }

    @Override
    public Long getAuditableId() {
        return id;
    }

    // --- Getters and Setters remain the same ---

    public Long getId() {
        return this.id;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getSuspiciousCount() {
        return this.suspiciousCount;
    }

    public String getLastGeolocation() {
        return this.lastGeolocation;
    }

    public Instant getFirstSeenAt() {
        return this.firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return this.lastSeenAt;
    }

    public List<String> getPreviousGeolocations() {
        return this.previousGeolocations;
    }

    public int getLastTtl() {
        return this.lastTtl;
    }

    public List<GeoLocationEntry> getGeoHistory() {
        return this.geoHistory;
    }

    public List<Integer> getTtlHistory() {
        return this.ttlHistory;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setSuspiciousCount(int suspiciousCount) {
        this.suspiciousCount = suspiciousCount;
    }

    public void setLastGeolocation(String lastGeolocation) {
        this.lastGeolocation = lastGeolocation;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void setPreviousGeolocations(List<String> previousGeolocations) {
        // Ensure a serializable list type is used if replacing the list
        this.previousGeolocations = (previousGeolocations != null) ? new ArrayList<>(previousGeolocations) : new ArrayList<>();
    }

    public void setLastTtl(int lastTtl) {
        this.lastTtl = lastTtl;
    }

    public void setGeoHistory(List<GeoLocationEntry> geoHistory) {
        // Ensure a serializable list type is used if replacing the list
        this.geoHistory = (geoHistory != null) ? new ArrayList<>(geoHistory) : new ArrayList<>();
    }

    public void setTtlHistory(List<Integer> ttlHistory) {
        // Ensure a serializable list type is used if replacing the list
        this.ttlHistory = (ttlHistory != null) ? new ArrayList<>(ttlHistory) : new ArrayList<>();
    }

    // --- toString, constructor, equals, hashCode remain the same ---

    public String toString() {
        return "IpMetadataEntity(id=" + this.getId() + ", ipAddress=" + this.getIpAddress() + ", suspiciousCount=" + this.getSuspiciousCount() + ", lastGeolocation=" + this.getLastGeolocation() + ", firstSeenAt=" + this.getFirstSeenAt() + ", lastSeenAt=" + this.getLastSeenAt() + ", previousGeolocations=" + this.getPreviousGeolocations() + ", lastTtl=" + this.getLastTtl() + ", geoHistory=" + this.getGeoHistory() + ", ttlHistory=" + this.getTtlHistory() + ")";
    }

    public IpMetadataEntity(String ipAddress) {
        this.ipAddress = ipAddress;
        this.firstSeenAt = Instant.now();
        this.lastSeenAt = Instant.now();
        this.suspiciousCount = 0;
        // Initialize lastTtl if needed, e.g., this.lastTtl = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpMetadataEntity that)) return false;
        // Consider if BaseEntity's equals/hashCode should be included if it defines fields
        return suspiciousCount == that.suspiciousCount && lastTtl == that.lastTtl && Objects.equals(id, that.id) && Objects.equals(ipAddress, that.ipAddress) && Objects.equals(lastGeolocation, that.lastGeolocation) && Objects.equals(firstSeenAt, that.firstSeenAt) && Objects.equals(lastSeenAt, that.lastSeenAt) && Objects.equals(previousGeolocations, that.previousGeolocations) && Objects.equals(geoHistory, that.geoHistory) && Objects.equals(ttlHistory, that.ttlHistory);
    }

    @Override
    public int hashCode() {
        // Consider if BaseEntity's equals/hashCode should be included
        return Objects.hash(id, ipAddress, suspiciousCount, lastGeolocation, firstSeenAt, lastSeenAt, previousGeolocations, lastTtl, geoHistory, ttlHistory);
    }
}