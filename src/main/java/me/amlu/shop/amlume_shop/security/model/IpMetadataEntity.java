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
import lombok.*;
import me.amlu.shop.amlume_shop.model.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Builder
@Entity
@Table(name = "ip_metadata", indexes = {
        @Index(name = "idx_ip_address", columnList = "ip_address"),
        @Index(name = "idx_suspicious_count", columnList = "suspicious_count"),
        @Index(name = "idx_last_seen_at", columnList = "last_seen_at")
})
@Cacheable
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
public class IpMetadataEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false, unique = true)
    private String ipAddress;

    @Column(name = "suspicious_count", nullable = false)
    @Builder.Default
    private int suspiciousCount = 0;

    @Column(name = "last_geolocation", nullable = false)
    private String lastGeolocation;

    @Column(name = "first_seen_at", nullable = false)
    @Builder.Default
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private Instant lastSeenAt = Instant.now();

    @ElementCollection
    @CollectionTable(name = "ip_previous_geolocations")
    private List<String> previousGeolocations = new ArrayList<>();

    @Column(name = "last_ttl", nullable = false)
    private int lastTtl;

    @ElementCollection
    @CollectionTable(name = "ip_geo_history")
    private List<GeoLocationEntry> geoHistory = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ip_ttl_history")
    private List<Integer> ttlHistory = new ArrayList<>();

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocationEntry {
        private String location;
        private Instant timestamp;
    }

    public IpMetadataEntity(String ipAddress) {
        this.ipAddress = ipAddress;
        this.firstSeenAt = Instant.now();
        this.lastSeenAt = Instant.now();
        this.suspiciousCount = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpMetadataEntity that)) return false;
        return suspiciousCount == that.suspiciousCount && lastTtl == that.lastTtl && Objects.equals(id, that.id) && Objects.equals(ipAddress, that.ipAddress) && Objects.equals(lastGeolocation, that.lastGeolocation) && Objects.equals(firstSeenAt, that.firstSeenAt) && Objects.equals(lastSeenAt, that.lastSeenAt) && Objects.equals(previousGeolocations, that.previousGeolocations) && Objects.equals(geoHistory, that.geoHistory) && Objects.equals(ttlHistory, that.ttlHistory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ipAddress, suspiciousCount, lastGeolocation, firstSeenAt, lastSeenAt, previousGeolocations, lastTtl, geoHistory, ttlHistory);
    }
}
