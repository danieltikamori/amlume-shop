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

import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.repositories.AsnRepository;
import me.amlu.shop.amlume_shop.security.model.AsnEntry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Write-through cache implementation for ASN (Autonomous System Number) lookup.
 * This class is responsible for caching ASN (Autonomous System Number) information for IP addresses.
 * It uses a write-through cache strategy, meaning that any cache miss will trigger a lookup and subsequent storage in the cache and database.
 * This ensures that the cache is always up-to-date with the latest ASN information.
 *
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @see AsnLookupServiceImpl
 * @see AsnRepository
 * @see Cache
 * @since 2025-02-16
 */

@Slf4j
@Service
@Transactional
public class WriteThroughAsnCacheServiceImpl implements WriteThroughAsnCacheService {
    private final Cache<String, String> cache;
    private final AsnLookupService lookupService;
    private final AsnRepository asnRepository;

    public WriteThroughAsnCacheServiceImpl(Cache<String, String> cache, AsnLookupService lookupService, AsnRepository asnRepository) {
        this.cache = cache;
        this.lookupService = lookupService;
        this.asnRepository = asnRepository;
    }


    @Override
    public String getAsn(String ip) {
        try {
            return cache.get(ip, () -> {
                // First check the database
                Optional<AsnEntry> existingEntry = asnRepository.findByIp(ip);
                if (existingEntry.isPresent()) {
                    return existingEntry.get().getAsn();
                }

                // If not in database, lookup and save
                String asn = lookupService.lookupAsn(ip);
                if (asn != null) {
                    asnRepository.save(new AsnEntry(ip, asn));
                }
                return asn;
            });
        } catch (ExecutionException e) {
            log.error("Error getting ASN for IP: {}", ip, e);
            return null;
        }
    }


//    @Override
//    public String getAsn(String ip) {
//        try {
//            return cache.get(ip, new Callable<String>() {
//                @Override
//                public String call() {
//                    // First check the database
//                    Optional<AsnEntry> existingEntry = asnRepository.findByIp(ip);
//                    if (existingEntry.isPresent()) {
//                        return existingEntry.get().getAsn();
//                    }
//
//                    // If not in database, lookup and save
//                    String asn = lookupService.lookupAsn(ip);
//                    if (asn != null) {
//                        asnRepository.save(new AsnEntry(ip, asn));
//                    }
//                    return asn;
//                }
//            });
//        } catch (ExecutionException e) {
//            log.error("Error getting ASN for IP: {}", ip, e);
//            return null;
//        }
//    }

    @Scheduled(cron = "${asn.cleanup.schedule:0 0 0 * * *}")
    public void cleanupStaleEntries() {
        LocalDateTime staleThreshold = LocalDateTime.now().minusDays(30);
        asnRepository.deleteStaleEntries(staleThreshold);
    }
}
