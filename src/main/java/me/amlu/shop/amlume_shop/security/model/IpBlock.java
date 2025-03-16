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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import me.amlu.shop.amlume_shop.model.BaseEntity;

import java.time.Instant;
import java.util.Objects;


@Entity
@Table(name = "ip_blocks", indexes = {
        @Index(name = "idx_ip_address", columnList = "ip_address")
})
@Cacheable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpBlock extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name= "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "blocked_at",  nullable = false)
    private Instant blockedAt;

    @Column(nullable = false)
    private boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpBlock ipBlock)) return false;
        return active == ipBlock.active && Objects.equals(id, ipBlock.id) && Objects.equals(ipAddress, ipBlock.ipAddress) && Objects.equals(reason, ipBlock.reason) && Objects.equals(blockedAt, ipBlock.blockedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ipAddress, reason, blockedAt, active);
    }
}