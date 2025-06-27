/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.repository;


import me.amlu.authserver.security.model.SecurityEvent;
import me.amlu.authserver.security.model.SecurityEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long>,
        JpaSpecificationExecutor<SecurityEvent> {

    // Find all events for a specific user
    List<SecurityEvent> findByUsername(String username);

    Page<SecurityEvent> findByUsername(String username, Pageable pageable);

    List<SecurityEvent> findByUserId(String userId);

    Page<SecurityEvent> findByIpAddress(String ipAddress, Pageable pageable);

    // Find events by type
    List<SecurityEvent> findByEventType(SecurityEventType eventType);

    // Find events within a timestamp range
    List<SecurityEvent> findByTimestampBetween(Instant startTime, Instant endTime);

    // Find events by user and type
    List<SecurityEvent> findByUsernameAndEventType(String username, SecurityEventType eventType);

    // Find recent events for a user
    @Query("SELECT e FROM SecurityEvent e WHERE e.username = :username AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<SecurityEvent> findRecentEventsByUsername(
            @Param("username") String username,
            @Param("since") Instant since
    );

    // Find failed login attempts within timestamp window
    @Query("SELECT e FROM SecurityEvent e WHERE e.username = :username AND e.eventType = 'FAILED_LOGIN' AND e.timestamp >= :since")
    List<SecurityEvent> findRecentFailedLogins(
            @Param("username") String username,
            @Param("since") Instant since
    );

    // Count failed login attempts within timestamp window
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.username = :username AND e.eventType = 'FAILED_LOGIN' AND e.timestamp >= :since")
    long countRecentFailedLogins(
            @Param("username") String username,
            @Param("since") Instant since
    );

    // Find suspicious activities by IP
    List<SecurityEvent> findByIpAddressAndEventType(String ipAddress, SecurityEventType eventType);

    // Find latest events
    @Query("SELECT e FROM SecurityEvent e WHERE e.timestamp >= :since ORDER BY e.timestamp DESC")
    Page<SecurityEvent> findLatestEvents(
            @Param("since") Instant since,
            Pageable pageable
    );

    // Delete old events
    @Modifying
    @Query("DELETE FROM SecurityEvent e WHERE e.timestamp < :before")
    void deleteEventsOlderThan(@Param("before") Instant before);

    // Find events by multiple types
    List<SecurityEvent> findByEventTypeIn(Collection<SecurityEventType> eventTypes);

    // Find events by IP address within timestamp range
    List<SecurityEvent> findByIpAddressAndTimestampBetween(
            String ipAddress,
            Instant startTime,
            Instant endTime
    );

    Page<SecurityEvent> findAll(Pageable pageable);
}
