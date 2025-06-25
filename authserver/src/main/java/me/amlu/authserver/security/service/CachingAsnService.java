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

import io.micrometer.core.annotation.Timed;
import me.amlu.authserver.security.config.properties.AsnProperties;
import me.amlu.authserver.security.model.AsnEntry;
import me.amlu.authserver.security.repository.AsnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static me.amlu.authserver.common.CacheKeys.ASN_CACHE;

/**
 * Service responsible for looking up ASN information for IP addresses,
 * utilizing Spring Cache (Valkey) and persisting results to a database.
 * Replaces the Guava-based WriteThroughAsnCacheServiceImpl.
 */
@Service
public class CachingAsnService {

    private static final Logger log = LoggerFactory.getLogger(CachingAsnService.class);

    private final AsnLookupService externalLookupServiceDelegate; // The service doing the actual lookup (with retries/rate limiting)
    private final AsnRepository asnRepository;
    private final AsnProperties asnProperties; // Inject properties for cleanup config

    public CachingAsnService(
            // Inject the service that includes retries and rate limiting, but NOT caching yet.
            @Qualifier("retryingAsnLookup") AsnLookupService externalLookupServiceDelegate,
            AsnRepository asnRepository,
            AsnProperties asnProperties // Inject properties
    ) {
        this.externalLookupServiceDelegate = externalLookupServiceDelegate;
        this.asnRepository = asnRepository;
        this.asnProperties = asnProperties;
        log.info("CachingAsnService initialized, using delegate: {}", externalLookupServiceDelegate.getClass().getSimpleName());
    }

    /**
     * Gets the ASN for an IP address.
     * Uses Spring Cache (Constants.ASN_CACHE). On cache miss, checks the database.
     * On database miss, performs an external lookup via the delegate service,
     * saves the result to the database, and caches the result.
     *
     * @param ip The IP address string.
     * @return The ASN string (e.g., "AS15169"), or null if not found after all steps.
     */
    @Cacheable(value = ASN_CACHE, key = "#ip", unless = "#result == null")
    @Transactional(readOnly = true) // Read-only by default for the lookup part
    @Timed(value = "authserver.asn.lookup", longTask = true, extraTags = {"ip"}, description = "Time taken to look up the ASN for an IP address.")
    public String getAsnWithCaching(String ip) {
        log.debug("Cache miss for ASN lookup for IP: {}. Checking database.", ip);

        // 1. Check Database
        Optional<AsnEntry> dbEntry = asnRepository.findByIp(ip);
        if (dbEntry.isPresent()) {
            log.debug("Found ASN '{}' in database for IP: {}", dbEntry.get().getAsn(), ip);
            // Optionally update lastUpdated timestamp even on DB hit?
            // updateLastUpdatedTimestamp(dbEntry.get()); // Consider if needed
            return dbEntry.get().getAsn();
        }

        log.debug("ASN not found in database for IP: {}. Performing external lookup via delegate.", ip);

        // 2. External Lookup via Delegate (includes retries/rate limiting)
        String asn = null;
        try {
            // Use the injected delegate which handles retries/rate limits
            asn = externalLookupServiceDelegate.lookupAsn(ip);
        } catch (Exception e) {
            // Catch exceptions from the delegate (e.g., RateLimitExceededException, or others after retries failed)
            log.error("External ASN lookup via delegate failed for IP: {}", ip, e);
            // Returning null here ensures failure isn't cached due to 'unless="#result == null"'
            return null;
        }

        // 3. Save to Database if found externally
        if (asn != null) {
            log.debug("External lookup successful for IP: {}. Saving ASN '{}' to database.", ip, asn);
            try {
                // Use helper to handle potential race conditions and transaction propagation
                saveAsnEntry(ip, asn);
            } catch (Exception e) {
                // Log DB save error but still return the ASN found this time
                log.error("Failed to save ASN entry to database for IP: {}. ASN '{}' will be cached but might not be persisted.", ip, asn, e);
            }
        } else {
            log.warn("External ASN lookup via delegate returned null for IP: {}", ip);
        }

        // 4. Return result (Spring will cache it if not null)
        return asn;
    }

    /**
     * Saves the ASN entry to the database. Marked potentially with REQUIRES_NEW
     * if saving should be independent of the main lookup transaction,
     * otherwise default REQUIRED is fine. Checks existence before saving.
     */
    @Transactional(propagation = Propagation.REQUIRED) // Or REQUIRES_NEW
    // Make protected or private if only called internally
    protected void saveAsnEntry(String ip, String asn) {
        // Check existence within this transaction before saving
        if (asnRepository.findByIp(ip).isEmpty()) {
            AsnEntry newEntry = new AsnEntry(ip, asn);
            asnRepository.save(newEntry);
            log.debug("Successfully saved new ASN entry for IP: {}", ip);
        } else {
            log.debug("ASN entry for IP {} already exists in DB, skipping save.", ip);
            // Optionally update the lastUpdated timestamp of the existing entry here
            // asnRepository.findByIp(ip).ifPresent(this::updateLastUpdatedTimestamp);
        }
    }

    // Optional: Helper to update timestamp if needed on DB hit or skip-save
    // @Transactional(propagation = Propagation.REQUIRES_NEW)
    // protected void updateLastUpdatedTimestamp(AsnEntry entry) {
    //     entry.setLastUpdated(LocalDateTime.now());
    //     asnRepository.save(entry);
    //     log.debug("Updated lastUpdated timestamp for ASN entry IP: {}", entry.getIp());
    // }


    /**
     * Scheduled task to clean up stale ASN entries from the database
     * based on the configured stale threshold and schedule.
     */
    @Scheduled(cron = "${asn.cleanup.schedule:0 0 3 * * *}") // Default: 3 AM daily
    @Transactional
    public void cleanupStaleAsnEntries() {
        log.info("Running scheduled cleanup of stale ASN entries...");
        // Use Duration from AsnProperties
        LocalDateTime staleThreshold = LocalDateTime.now().minus(asnProperties.getStaleThreshold());
        try {
            // Assuming deleteStaleEntries returns void or int count
            asnRepository.deleteStaleEntries(staleThreshold);
            // If it returned count: int deletedCount = asnRepository.deleteStaleEntries(staleThreshold);
            log.info("Completed stale ASN entry cleanup for entries older than {}", staleThreshold);
        } catch (Exception e) {
            log.error("Error during scheduled ASN entry cleanup", e);
        }
    }
}
