/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.IpSecurityException;
import me.amlu.shop.amlume_shop.security.model.IpBlock;
import me.amlu.shop.amlume_shop.security.model.IpMetadata;
import me.amlu.shop.amlume_shop.security.model.IpMetadataEntity;
import me.amlu.shop.amlume_shop.security.repository.IpBlockRepository;
import me.amlu.shop.amlume_shop.security.repository.IpMetadataRepository;
import me.amlu.shop.amlume_shop.security.repository.IpWhitelistRepository;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@Transactional
public class IpSecurityServiceImpl implements IpSecurityService {
    private static final int CACHE_SIZE = 10000;
    private static final Duration CACHE_DURATION = Duration.ofHours(24);

    private final IpBlockRepository ipBlockRepository;
    private final IpWhitelistRepository ipWhitelistRepository;
    private final IpMetadataRepository ipMetadataRepository;
    private final GeoLocationService geoLocationService;
    private final AuditLogger auditLogger;
    private final LoadingCache<String, Boolean> ipBlockCache;
    private final LoadingCache<String, IpMetadata> ipMetadataCache;
    private final RateLimiter rateLimiter;
    private IpSecurityService self;

    @Value("${security.ip.suspicious-requests-threshold}")
    private int suspiciousRequestsThreshold;

    @Value("${security.ip.block-threshold}")
    private int blockThreshold;

    public IpSecurityServiceImpl(IpBlockRepository ipBlockRepository, IpWhitelistRepository ipWhitelistRepository,
                                 IpMetadataRepository ipMetadataRepository,
                                 GeoLocationService geoLocationService,
                                 AuditLogger auditLogger) {
        this.ipBlockRepository = ipBlockRepository;
        this.ipWhitelistRepository = ipWhitelistRepository;
        this.ipMetadataRepository = ipMetadataRepository;
        this.geoLocationService = geoLocationService;
        this.auditLogger = auditLogger;

        this.ipBlockCache = CacheBuilder.newBuilder()
                .expireAfterWrite(CACHE_DURATION)
                .maximumSize(CACHE_SIZE)
                .recordStats()
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public Boolean load(@NotNull String ip) {
                        return checkIfIpIsBlocked(ip);
                    }
                });

        this.ipMetadataCache = CacheBuilder.newBuilder()
                .expireAfterWrite(CACHE_DURATION)
                .maximumSize(CACHE_SIZE)
                .recordStats()
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public IpMetadata load(@NotNull String ip) {
                        return getOrCreateIpMetadata(ip);
                    }
                });

        this.rateLimiter = RateLimiter.create(100.0); // 100 requests per second
    }

    @Override
    public boolean isIpBlocked(String ip) {
        if (StringUtils.isBlank(ip)) {
            log.warn("Attempt to check blank IP address");
            return true;
        }

        try {
            // Check if IP is explicitly blocked
            if (Optional.of(ipBlockCache.get(ip)).orElse(false)) {
                return true;
            }

            // Check if IP is suspicious enough to be blocked
            IpMetadata metadata = ipMetadataCache.get(ip);
            if (metadata.getSuspiciousCount() >= blockThreshold) {
                self.blockIp(ip, "Exceeded suspicious activity threshold");
                return true;
            }

            return false;
        } catch (ExecutionException e) {
            log.error("Error checking IP block status: {}", ip, e);
            return true; // Fail secure
        }
    }

    @Override
    public boolean isIpSuspicious(String ip, HttpServletRequest request) {
        if (StringUtils.isBlank(ip)) {
            log.warn("Attempt to check blank IP address for suspicion");
            return true;
        }

        try {
            IpMetadata metadata = ipMetadataCache.get(ip);
            String currentGeo = geoLocationService.getGeolocation(ip);
            int currentTtl = extractTTL(request);

            boolean suspicious = checkSuspiciousActivity(ip, currentGeo, currentTtl, metadata);

            if (suspicious && metadata.getSuspiciousCount() >= blockThreshold) {
                self.blockIp(ip, "Exceeded suspicious activity threshold");
            }

            // Persist updated metadata
            self.persistIpMetadata(ip, metadata);
            return suspicious;

        } catch (Exception e) {
            log.error("Error checking IP suspicion: {}", ip, e);
            return true; // Fail safe
        }
    }

    private boolean checkSuspiciousActivity(String ip, String currentGeo, int currentTtl, IpMetadata metadata) {
        boolean suspicious = false;

        // Check for geolocation changes
        if (metadata.hasGeolocationChanged(currentGeo)) {
            log.warn("Suspicious geolocation change detected for IP: {}", ip);
            metadata.incrementSuspiciousCount();
            suspicious = true;
        }

        // Check for TTL anomalies
        if (metadata.hasTTLAnomaly(currentTtl)) {
            log.warn("TTL anomaly detected for IP: {}", ip);
            metadata.incrementSuspiciousCount();
            suspicious = true;
        }

        // Check rate limiting
        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            metadata.incrementSuspiciousCount();
            suspicious = true;
        }

        metadata.updateLastSeen();
        return suspicious || metadata.getSuspiciousCount() > suspiciousRequestsThreshold;
    }

    @Override
    @Transactional
    public void blockIp(String ip, String reason) {
        if (StringUtils.isBlank(ip)) {
            return;
        }

        try {
            IpBlock ipBlock = IpBlock.builder()
                    .ipAddress(ip)
                    .reason(reason)
                    .blockedAt(Instant.now())
                    .active(true)
                    .build();

            ipBlockRepository.save(ipBlock);
            ipBlockCache.put(ip, true);

            auditLogger.logSecurityEvent("IP_BLOCKED", null, ip, reason);
            log.info("IP address blocked: {} for reason: {}", ip, reason);

        } catch (Exception e) {
            log.error("Error blocking IP address: {}", ip, e);
            throw new IpSecurityException("Failed to block IP address", e);
        }
    }

    private int extractTTL(HttpServletRequest request) {
        try {
            String ttlHeader = request.getHeader("X-TTL");
            return ttlHeader != null ? Integer.parseInt(ttlHeader) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Transactional
    @Override
    public void persistIpMetadata(String ip, IpMetadata metadata) {
        try {
            IpMetadataEntity entity = ipMetadataRepository.findByIpAddress(ip)
                    .orElseGet(() -> new IpMetadataEntity(ip));

            updateEntityFromMetadata(entity, metadata);
            ipMetadataRepository.save(entity);

        } catch (Exception e) {
            log.error("Error persisting IP metadata for IP: {}", ip, e);
            // Handle the error appropriately
        }
    }

    private IpMetadata getOrCreateIpMetadata(String ip) {
        return ipMetadataRepository.findByIpAddress(ip)
                .map(this::convertToIpMetadata)
                .orElseGet(IpMetadata::new);
    }

    private IpMetadata convertToIpMetadata(IpMetadataEntity entity) {
        IpMetadata metadata = new IpMetadata();
        // Set the values from entity to metadata
        metadata.setLastGeolocation(entity.getLastGeolocation());
        entity.getPreviousGeolocations().forEach(metadata::setLastGeolocation);

        // Set timestamps
        metadata.setFirstSeen(entity.getFirstSeenAt());
        metadata.setLastSeen(entity.getLastSeenAt());

        // Set TTL
        for (Integer ttl : entity.getTtlHistory()) {
            metadata.hasTTLAnomaly(ttl); // This will populate the TTL history
        }

        // Set Geo history
        for (IpMetadataEntity.GeoLocationEntry entry : entity.getGeoHistory()) {
            metadata.hasGeolocationChanged(entry.getLocation()); // This will populate the geo history
        }

        // Set suspicious count
        for (int i = 0; i < entity.getSuspiciousCount(); i++) {
            metadata.incrementSuspiciousCount();
        }

        // Set other fields as needed

        return metadata;
    }

    private Boolean checkIfIpIsBlocked(String ip) {
        return ipBlockRepository.existsByIpAddressAndActiveTrue(ip);
    }

    private void updateEntityFromMetadata(IpMetadataEntity entity, IpMetadata metadata) {
        entity.setSuspiciousCount(metadata.getSuspiciousCount());
        entity.setLastGeolocation(metadata.getLastGeolocation());
        entity.setLastSeenAt(metadata.getLastSeen());
        entity.setPreviousGeolocations(new ArrayList<>(metadata.getPreviousGeolocations()));

        // Update geo history
        List<IpMetadataEntity.GeoLocationEntry> geoEntries = new ArrayList<>();
        for (IpMetadata.GeoLocation loc : metadata.getGeoHistory()) {
            geoEntries.add(new IpMetadataEntity.GeoLocationEntry(
                    loc.location(),
                    loc.timestamp()
            ));
        }
        entity.setGeoHistory(geoEntries);

        // Update TTL history
        entity.setTtlHistory(new ArrayList<>(metadata.getTtlHistory()));
        entity.setLastTtl(metadata.getLastTtl());
    }

    @Override
    @Transactional
    public void unblockIp(String userId, String ip) {
        if (StringUtils.isBlank(ip)) {
            return;
        }
        try {
            ipBlockRepository.deactivateIpBlock(ip);
//            ipBlockRepository.deleteByIpAddress(ip);
//            // Clear metadata cache for this IP
            ipMetadataCache.invalidate(ip);
            log.info("IP address unblocked: {}", ip);
            auditLogger.logSecurityEvent("IP_UNBLOCKED", userId, "Manual unblock", ip);
        } catch (Exception e) {
            log.error("Error unblocking IP address: {}", ip, e);
            throw new IpSecurityException("Failed to unblock IP address", e);
        }
    }
    @Override
    public boolean isIpWhitelisted(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            return false;
        }

        try {
            return ipWhitelistRepository.existsByIpAddressAndActiveTrue(ipAddress);
        } catch (Exception e) {
            log.error("Error checking IP whitelist status for IP: {}", ipAddress, e);
            return false;
        }
    }


    // Helper method to get metadata for testing/monitoring
    @VisibleForTesting
    IpMetadata getIpMetadata(String ip) throws ExecutionException {
        return ipMetadataCache.get(ip);
    }

//    public boolean isIpSuspicious(String ip, HttpServletRequest request) {
//        IpMetadata metadata = getIpMetadata(ip);
//
//        // Check for common spoofing indicators
//        boolean suspicious = checkSpoofingIndicators(ip, request, metadata);
//
//        if (suspicious) {
//            metadata.incrementSuspiciousCount();
//            if (metadata.getSuspiciousCount() > suspiciousRequestsThreshold) {
//                blockIp(ip);
//                return true;
//            }
//        }
//
//        return false;
//    }

//    private boolean checkSpoofingIndicators(String ip, HttpServletRequest request, IpMetadata metadata) {
//        // Check for rapid geolocation changes
//        String currentGeo = String.valueOf(geoLocationService.getGeolocation(ip));
//        if (metadata.hasGeolocationChanged(currentGeo)) {
//            log.warn("Suspicious geolocation change for IP: {}", ip);
//            return true;
//        }
//
//        // Check for inconsistent TTL values
//        int ttl = extractTTL(request);
//        if (metadata.hasTTLAnomaly(ttl)) {
//            log.warn("TTL anomaly detected for IP: {}", ip);
//            return true;
//        }
//
//        // Check for TCP sequence prediction
//        if (hasAbnormalTcpSequence(request)) {
//            log.warn("Abnormal TCP sequence for IP: {}", ip);
//            return true;
//        }
//
//        return false;
//    }
}

