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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AsnReputationServiceImpl implements AsnReputationService {
    private final Map<String, AsnReputation> asnReputations = new ConcurrentHashMap<>();
    
    @Data
    public class AsnReputation {
        private final String asn;
        private AtomicInteger suspiciousActivityCount = new AtomicInteger(0);
        private AtomicInteger legitimateActivityCount = new AtomicInteger(0);
        private LocalDateTime lastUpdated = LocalDateTime.now();
        
        public double getReputationScore() {
            int total = suspiciousActivityCount.get() + legitimateActivityCount.get();
            if (total == 0) return 0.5; // Neutral score for new ASNs
            return (double) legitimateActivityCount.get() / total;
        }
    }

    @Override
    public void recordActivity(String asn, boolean isSuspicious) {
        AsnReputation reputation = asnReputations.computeIfAbsent(asn, 
            k -> new AsnReputation(k));
            
        if (isSuspicious) {
            reputation.getSuspiciousActivityCount().incrementAndGet();
        } else {
            reputation.getLegitimateActivityCount().incrementAndGet();
        }
        reputation.setLastUpdated(LocalDateTime.now());
    }

    @Override
    public double getReputationScore(String asn) {
        return asnReputations.getOrDefault(asn, new AsnReputation(asn))
                            .getReputationScore();
    }
    
    @Scheduled(cron = "${asn.reputation.decay.schedule}")
    public void applyReputationDecay() {
        // Implement reputation decay logic to give more weight to recent activities
        asnReputations.values().forEach(reputation -> {
            if (ChronoUnit.DAYS.between(reputation.getLastUpdated(),
                LocalDateTime.now()) > 30) {
                reputation.getSuspiciousActivityCount().updateAndGet(v -> (int)(v * 0.9));
                reputation.getLegitimateActivityCount().updateAndGet(v -> (int)(v * 0.9));
            }
        });
    }
}
