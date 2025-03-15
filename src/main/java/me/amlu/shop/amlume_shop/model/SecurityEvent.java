/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "security_events")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecurityEventType eventType;

    @Column(name = "user_id")
    @ToString.Exclude
    private String userId;

    @Column(nullable = false, name = "username")
    @ToString.Exclude
    private String username;

    @Column(name = "ip_address")
    @ToString.Exclude
    private String ipAddress;

    @Column(nullable = false, columnDefinition = "DATETIME ZONE='UTC'", name = "timestamp")
    private Instant timestamp;

    @Column(length = 1000, name = "details")
    @ToString.Exclude
    private String details;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityEvent that)) return false;
        return Objects.equals(id, that.id) && eventType == that.eventType && Objects.equals(userId, that.userId) && Objects.equals(username, that.username) && Objects.equals(ipAddress, that.ipAddress) && Objects.equals(timestamp, that.timestamp) && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventType, userId, username, ipAddress, timestamp, details);
    }
}
