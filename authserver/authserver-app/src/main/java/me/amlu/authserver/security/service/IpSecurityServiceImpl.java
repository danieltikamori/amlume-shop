/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import com.google.common.annotations.VisibleForTesting;
import jakarta.servlet.http.HttpServletRequest;

import me.amlu.authserver.common.StringUtils;
import me.amlu.authserver.exceptions.IpSecurityException;
import me.amlu.authserver.resilience.ratelimiter.RateLimiter;
import me.amlu.authserver.security.model.GeoLocationEntry;
import me.amlu.authserver.security.model.IpBlocklist;
import me.amlu.authserver.security.model.IpMetadata;
import me.amlu.authserver.security.model.IpMetadataEntity;
import me.amlu.authserver.security.repository.IpBlockRepository;
import me.amlu.authserver.security.repository.IpMetadataRepository;
import me.amlu.authserver.security.repository.IpWhitelistRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.amlu.authserver.common.CacheKeys.IP_BLOCK_CACHE;
import static me.amlu.authserver.common.CacheKeys.IP_METADATA_CACHE;

@Service
public class IpSecurityServiceImpl implements IpSecurityService {

    private static final String IP_SUSPICION_LIMITER_NAME = "ipSuspicionCheck";

    @Value("${security.ip.suspicious-requests-threshold}")
    private int suspiciousRequestsThreshold;

    @Value("${security.ip.block-threshold}")
    private int blockThreshold;

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IpSecurityServiceImpl.class);

    private final IpBlockRepository ipBlockRepository;
    private final IpWhitelistRepository ipWhitelistRepository;
    private final IpMetadataRepository ipMetadataRepository;
    private final GeoLocationService geoLocationService;
    private final AuditLogger auditLogger;
    private final RateLimiter rateLimiter; // Custom RateLimiter interface
    private final IpSecurityService self; // For transactional self-calls
    private final Cache ipBlockCacheInstance; // Spring Cache instance
    private final Cache ipMetadataCacheInstance; // Spring Cache instance

    public IpSecurityServiceImpl(
            IpBlockRepository ipBlockRepository,
            IpWhitelistRepository ipWhitelistRepository,
            IpMetadataRepository ipMetadataRepository,
            GeoLocationService geoLocationService,
            AuditLogger auditLogger,
            CacheManager cacheManager, // Inject CacheManager
            @Lazy IpSecurityService self, // Inject self lazily
            @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter
    ) {
        this.ipBlockRepository = ipBlockRepository;
        this.ipWhitelistRepository = ipWhitelistRepository;
        this.ipMetadataRepository = ipMetadataRepository;
        this.geoLocationService = geoLocationService;
        this.auditLogger = auditLogger;
        this.self = self; // Assign self
        this.rateLimiter = rateLimiter;

        // Initialize Spring Cache instances
        this.ipBlockCacheInstance = cacheManager.getCache(IP_BLOCK_CACHE);
        this.ipMetadataCacheInstance = cacheManager.getCache(IP_METADATA_CACHE);
        Objects.requireNonNull(this.ipBlockCacheInstance, "Cache '" + IP_BLOCK_CACHE + "' not configured or CacheManager not available.");
        Objects.requireNonNull(this.ipMetadataCacheInstance, "Cache '" + IP_METADATA_CACHE + "' not configured or CacheManager not available.");

        log.info("IpSecurityService initialized using RateLimiter implementation: {}.", rateLimiter.getClass().getSimpleName());
    }

    @Override
    public boolean isIpBlocked(String ip) {
        if (StringUtils.isBlank(ip)) {
            log.warn("Attempt to check blank IP address");
            return true; // Fail secure
        }

        try {
            // Check cache for explicit block
            // The second argument is a Callable used if the key is not found
            Boolean blocked = ipBlockCacheInstance.get(ip, () -> checkIfIpIsBlocked(ip));

            // Handle potential null from cache loader if checkIfIpIsBlocked could return null
            // Or if cache allows null values (though we disabled it)
            if (Boolean.TRUE.equals(blocked)) {
                return true;
            }

            // Check if IP is suspicious enough to be blocked (load metadata from cache)
            IpMetadata metadata = ipMetadataCacheInstance.get(ip, () -> getOrCreateIpMetadata(ip));

            // Handle case where metadata loading fails or returns null
            if (metadata == null) {
                log.error("Failed to load or create IP metadata for IP: {}. Assuming blocked.", ip);
                return true; // Fail secure
            }

            if (metadata.getSuspiciousCount() >= blockThreshold) {
                // Use self-injection to ensure transactional execution
                self.blockIp(ip, "Exceeded suspicious activity threshold");
                return true;
            }

            return false;
        } catch (Cache.ValueRetrievalException e) {
            // Handle exceptions during the cache loading phase (checkIfIpIsBlocked or getOrCreateIpMetadata)
            log.error("Error retrieving value from cache for IP: {}. Assuming blocked.", ip, e);
            return true; // Fail secure
        } catch (Exception e) {
            // Catch unexpected errors
            log.error("Unexpected error checking IP block status: {}. Assuming blocked.", ip, e);
            return true; // Fail secure
        }
    }

    @Override
    public boolean isIpSuspicious(String ip, HttpServletRequest request) {
        if (StringUtils.isBlank(ip)) {
            log.warn("Attempt to check blank IP address for suspicion");
            return true; // Fail-safe/secure based on policy
        }

        try {
            // Load metadata from cache or create if not present
            IpMetadata metadata = ipMetadataCacheInstance.get(ip, () -> getOrCreateIpMetadata(ip));

            if (metadata == null) {
                log.error("Failed to load or create IP metadata for IP: {}. Assuming suspicious.", ip);
                return true; // Fail safe/secure
            }

            // Get current geolocation (already cached by the service)
            String currentGeo = geoLocationService.getGeolocation(ip);

            int currentTtl = extractTTL(request);

            boolean suspicious = checkSuspiciousActivity(ip, currentGeo, currentTtl, metadata);

            if (suspicious && metadata.getSuspiciousCount() >= blockThreshold) {
                // Use self-injection for transactional call
                self.blockIp(ip, "Exceeded suspicious activity threshold");
                // No need to return true immediately after blocking, let persist happen
            }

            // Persist updated metadata (transactional via self-injection)
            self.persistIpMetadata(ip, metadata); // persistIpMetadata should update the cache if necessary
            return suspicious;

        } catch (Cache.ValueRetrievalException e) {
            log.error("Error retrieving metadata from cache for IP: {}. Assuming suspicious.", ip, e);
            return true; // Fail safe/secure
        } catch (Exception e) {
            log.error("Error checking IP suspicion: {}. Assuming suspicious.", ip, e);
            return true; // Fail safe/secure
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

        // Rate limit check
        // This is a custom rate limiter, not the Spring one
        String rateLimitKey = IP_SUSPICION_LIMITER_NAME + ":" + ip;

        try {
            if (!rateLimiter.tryAcquire(rateLimitKey)) {
                log.warn("Rate limit exceeded for IP: {}", ip);
                metadata.incrementSuspiciousCount();
                suspicious = true;
            }
        } catch (Exception e) {
            // Handle potential errors from the Redis rate limiter (e.g., connection issues)
            // Decide whether to count this as suspicious or ignore based on fail-open/closed policy
            // For now, let's log and potentially count as suspicious (fail-closed for suspicion check)
            log.error("Error checking internal rate limit for IP: {}. Counting as suspicious.", ip, e);
            metadata.incrementSuspiciousCount();
            suspicious = true;
        }

        metadata.updateLastSeen();
        // Return true if any check was suspicious OR if the count exceeds the lower threshold
        return suspicious || metadata.getSuspiciousCount() > suspiciousRequestsThreshold;
    }


    @Override
    @Transactional
    public void blockIp(String ip, String reason) {
        if (StringUtils.isBlank(ip)) {
            return;
        }

        try {
            // Check if already blocked to avoid redundant operations (optional)
            if (Boolean.TRUE.equals(ipBlockCacheInstance.get(ip, () -> checkIfIpIsBlocked(ip)))) {
                log.debug("IP {} already blocked, skipping block operation.", ip);
                return;
            }

            IpBlocklist ipBlocklist = IpBlocklist.builder()
                    .ipAddress(ip)
                    .reason(reason)
                    .blockedAt(Instant.now())
                    .active(true)
                    .build();

            ipBlockRepository.save(ipBlocklist);
            ipBlockCacheInstance.put(ip, true); // Update cache after successful save

            auditLogger.logSecurityEvent("IP_BLOCKED", null, ip, reason);
            log.info("IP address blocked: {} for reason: {}", ip, reason);

        } catch (Exception e) {
            // Don't re-throw IpSecurityException if already caught, just log
            log.error("Error blocking IP address: {}", ip, e);
            // Let Spring handle transaction rollback on runtime exceptions
            throw new IpSecurityException("Failed to block IP address: " + ip, e);
        }
    }

    private int extractTTL(HttpServletRequest request) {
        try {
            String ttlHeader = request.getHeader("X-TTL");
            // Provide a default TTL if header is missing, e.g., 0 or -1
            return ttlHeader != null ? Integer.parseInt(ttlHeader) : 0;
        } catch (NumberFormatException e) {
            log.warn("Invalid TTL header format for request from {}: {}", request.getRemoteAddr(), request.getHeader("X-TTL"));
            return 0; // Default value on format error
        }
    }

    @Override
    @Transactional
    public void persistIpMetadata(String ip, IpMetadata metadata) {
        if (StringUtils.isBlank(ip) || metadata == null) {
            log.warn("Attempted to persist null or blank IP/metadata");
            return;
        }
        try {
            IpMetadataEntity entity = ipMetadataRepository.findByIpAddress(ip)
                    .orElseGet(() -> new IpMetadataEntity(ip)); // Create if not found

            updateEntityFromMetadata(entity, metadata);
            IpMetadataEntity savedEntity = ipMetadataRepository.save(entity);

            // Update cache AFTER successful save
            // Convert the saved entity back to IpMetadata to ensure cache consistency
            ipMetadataCacheInstance.put(ip, convertToIpMetadata(savedEntity));
            log.debug("Persisted and updated cache for IP metadata: {}", ip);

        } catch (Exception e) {
            log.error("Error persisting IP metadata for IP: {}", ip, e);
            // Let Spring handle transaction rollback
            throw new IpSecurityException("Failed to persist IP metadata for: " + ip, e);
        }
    }

    // This is the loader function for the ipMetadataCache
    private IpMetadata getOrCreateIpMetadata(String ip) {
        log.debug("Cache miss or loading required for IP metadata: {}", ip);
        // Find existing or create a new transient IpMetadata object
        // This doesn't save to DB yet, persistIpMetadata does that.
        return ipMetadataRepository.findByIpAddress(ip)
                .map(this::convertToIpMetadata) // Convert existing entity
                .orElseGet(() -> {
                    log.debug("No existing metadata found for IP: {}, creating new transient instance.", ip);
                    return new IpMetadata(); // Create new transient metadata
                });
    }

    // This is the loader function for the ipBlockCache
    private Boolean checkIfIpIsBlocked(String ip) {
        log.debug("Cache miss or loading required for IP block status: {}", ip);
        return ipBlockRepository.existsByIpAddressAndActiveTrue(ip);
    }


    // --- Conversion methods remain the same ---
    private IpMetadata convertToIpMetadata(IpMetadataEntity entity) {
        IpMetadata metadata = new IpMetadata();
        metadata.setLastGeolocation(entity.getLastGeolocation());
        // This seems wrong - it overwrites lastGeolocation repeatedly.
        // Should likely add to a history list if IpMetadata supports it.
        // Assuming IpMetadata.setLastGeolocation is correct for now.
        // entity.getPreviousGeolocations().forEach(metadata::setLastGeolocation); // REVIEW THIS LOGIC

        // A better approach if IpMetadata has a way to add previous locations:
        if (entity.getPreviousGeolocations() != null) {
            entity.getPreviousGeolocations().forEach(geo -> {
                // Assuming IpMetadata has a method like addPreviousGeolocation(String geo)
                // metadata.addPreviousGeolocation(geo);
            });
        }

        // Assuming IpMetadata has a method to set the last TTL
        metadata.setFirstSeen(entity.getFirstSeenAt());
        metadata.setLastSeen(entity.getLastSeenAt());

        // Populate TTL history by calling hasTTLAnomaly (relies on side effect)
        if (entity.getTtlHistory() != null) {
            entity.getTtlHistory().forEach(metadata::hasTTLAnomaly);
        }

        // Populate Geo history by calling hasGeolocationChanged (relies on side effect)
        if (entity.getGeoHistory() != null) {
            entity.getGeoHistory().forEach(entry -> metadata.hasGeolocationChanged(entry.getLocation()));
        }

        // Set suspicious count
        // This is inefficient. IpMetadata should have a setSuspiciousCount method.
        // Assuming it does: metadata.setSuspiciousCount(entity.getSuspiciousCount());
        // If not, the loop is needed but suboptimal:
        for (int i = 0; i < entity.getSuspiciousCount(); i++) {
            metadata.incrementSuspiciousCount();
        }

        return metadata;
    }

    private void updateEntityFromMetadata(IpMetadataEntity entity, IpMetadata metadata) {
        entity.setSuspiciousCount(metadata.getSuspiciousCount());
        entity.setLastGeolocation(metadata.getLastGeolocation());
        entity.setLastSeenAt(metadata.getLastSeen());

        // Ensure lists are not null before creating new ArrayList
        entity.setPreviousGeolocations(metadata.getPreviousGeolocations() != null ? new ArrayList<>(metadata.getPreviousGeolocations()) : new ArrayList<>());

        // Update geohistory
        List<GeoLocationEntry> geoEntries = new ArrayList<>();
        if (metadata.getGeoHistory() != null) {
            for (IpMetadata.GeoLocation loc : metadata.getGeoHistory()) {
                // Ensure location and timestamp are not null if required by GeoLocationEntry constructor
                if (loc != null && loc.location() != null && loc.timestamp() != null) {
                    geoEntries.add(new GeoLocationEntry(
                            loc.location(),
                            loc.timestamp()
                    ));
                }
            }
        }
        entity.setGeoHistory(geoEntries);

        // Update TTL history
        entity.setTtlHistory(metadata.getTtlHistory() != null ? new ArrayList<>(metadata.getTtlHistory()) : new ArrayList<>());
        entity.setLastTtl(metadata.getLastTtl());
    }
    // --- End of conversion methods ---


    @Override
    @Transactional
    public void unblockIp(String userId, String ip) {
        if (StringUtils.isBlank(ip)) {
            return;
        }
        try {
            ipBlockRepository.deactivateIpBlock(ip);
            // Evict from caches after successful deactivation
            ipBlockCacheInstance.evict(ip);
            ipMetadataCacheInstance.evict(ip); // Evict metadata too, as block status changed

            log.info("IP address unblocked: {}", ip);
            auditLogger.logSecurityEvent("IP_UNBLOCKED", userId, "Manual unblock", ip);
        } catch (Exception e) {
            log.error("Error unblocking IP address: {}", ip, e);
            throw new IpSecurityException("Failed to unblock IP address: " + ip, e);
        }
    }

    @Override
    public boolean isIpWhitelisted(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            return false;
        }
        // No caching needed here unless whitelist changes very rarely and lookup is expensive
        try {
            return ipWhitelistRepository.existsByIpAddressAndActiveTrue(ipAddress);
        } catch (Exception e) {
            log.error("Error checking IP whitelist status for IP: {}", ipAddress, e);
            return false; // Fail-safe (treat as not whitelisted on error)
        }
    }


    // Helper method for testing/monitoring - needs adjustment for Spring Cache
    @VisibleForTesting
    IpMetadata getIpMetadata(String ip) {
        // Direct cache access for inspection (might return null if not loaded)
        Cache.ValueWrapper wrapper = ipMetadataCacheInstance.get(ip);
        return (wrapper != null) ? (IpMetadata) wrapper.get() : null;
        // Or trigger loading if needed for test:
        // return ipMetadataCacheInstance.get(ip, () -> getOrCreateIpMetadata(ip));
    }
}
