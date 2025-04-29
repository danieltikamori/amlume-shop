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

import jakarta.persistence.criteria.Predicate;
import me.amlu.shop.amlume_shop.model.SecurityEvent;
import me.amlu.shop.amlume_shop.model.SecurityEventType;
import me.amlu.shop.amlume_shop.repositories.SecurityEventRepository;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SecurityEventService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SecurityEventService.class);
    private final SecurityEventRepository securityEventRepository;
    Instant retentionDate = Instant.now().minus(Duration.ofDays(90));

    public SecurityEventService(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    public SecurityEvent createEvent(
            SecurityEventType type,
            String username,
            String ipAddress,
            String details) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(type)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .details(details)
                .build();

        return securityEventRepository.save(event);
    }

    public List<SecurityEvent> getUserEvents(String username) {
        return securityEventRepository.findByUsername(username);
    }

    public long getRecentFailedLoginAttempts(String username, Duration window) {
        Instant since = Instant.now().minus(window);
        return securityEventRepository.countRecentFailedLogins(username, since);
    }

    public List<SecurityEvent> getRecentEvents(Duration window) {
        Instant since = Instant.now().minus(window);
        return securityEventRepository.findLatestEvents(
                since,
                PageRequest.of(0, 100, Sort.by("timestamp").descending())
        ).getContent();
    }

    @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
    public void cleanupOldEvents() {
        try {

            securityEventRepository.deleteEventsOlderThan(retentionDate);
            log.info("Successfully cleaned up old security events");
        } catch (Exception e) {
            log.error("Error cleaning up old security events", e);
        }
    }

    public List<SecurityEvent> findSuspiciousActivities(String ipAddress) {
        return securityEventRepository.findByIpAddressAndEventType(
                ipAddress,
                SecurityEventType.SUSPICIOUS_ACTIVITY
        );
    }

    public Page<SecurityEvent> getSecurityEventHistory(
            String username,
            Instant startTime,
            Instant endTime,
            Pageable pageable) {
        return securityEventRepository.findAll(
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    if (username != null) {
                        predicates.add(cb.equal(root.get("username"), username));
                    }

                    if (startTime != null) {
                        predicates.add(cb.greaterThanOrEqualTo(
                                root.get("timestamp"),
                                startTime
                        ));
                    }

                    if (endTime != null) {
                        predicates.add(cb.lessThanOrEqualTo(
                                root.get("timestamp"),
                                endTime
                        ));
                    }

                    return cb.and(predicates.toArray(new Predicate[0]));
                },
                pageable
        );
    }
}
