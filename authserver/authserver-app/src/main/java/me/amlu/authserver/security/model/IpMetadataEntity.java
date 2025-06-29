/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.amlu.authserver.model.BaseEntity;

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
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"previousGeolocations", "geoHistory", "ttlHistory"})
// Exclude collections from default toString
public class IpMetadataEntity extends BaseEntity<String> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

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

    @Column(name = "last_ttl")
    private int lastTtl;

    @ElementCollection(fetch = FetchType.LAZY) // Use LAZY fetch for collections
    @CollectionTable(name = "ip_previous_geolocations", joinColumns = @JoinColumn(name = "ip_metadata_id"))
    private List<String> previousGeolocations = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ip_geo_history", joinColumns = @JoinColumn(name = "ip_metadata_id"))
    private List<GeoLocationEntry> geoHistory = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ip_ttl_history", joinColumns = @JoinColumn(name = "ip_metadata_id"))
    private List<Integer> ttlHistory = new ArrayList<>();

    /**
     * Convenience constructor for creating a new metadata entry for an IP address.
     *
     * @param ipAddress The IP address.
     */
    public IpMetadataEntity(String ipAddress) {
        this.ipAddress = ipAddress;
        this.firstSeenAt = Instant.now();
        this.lastSeenAt = Instant.now();
        this.suspiciousCount = 0;
        this.lastGeolocation = "Unknown"; // FIX: Initialize non-nullable field
    }

    @Override
    public String getAuditableId() {
        return id;
    }

    // Defensive setters for collections
    public void setPreviousGeolocations(List<String> previousGeolocations) {
        this.previousGeolocations = (previousGeolocations != null) ? new ArrayList<>(previousGeolocations) : new ArrayList<>();
    }

    public void setGeoHistory(List<GeoLocationEntry> geoHistory) {
        this.geoHistory = (geoHistory != null) ? new ArrayList<>(geoHistory) : new ArrayList<>();
    }

    public void setTtlHistory(List<Integer> ttlHistory) {
        this.ttlHistory = (ttlHistory != null) ? new ArrayList<>(ttlHistory) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IpMetadataEntity that = (IpMetadataEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(ipAddress, that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ipAddress);
    }
}
