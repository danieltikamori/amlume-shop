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
import me.amlu.shop.amlume_shop.model.BaseEntity;

import java.time.Instant;
import java.util.Objects;


@Entity
@Table(name = "ip_blocks", indexes = {
        @Index(name = "idx_ip_block_address", columnList = "ip_address")
})
@Cacheable
public class IpBlock extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    @Column(nullable = false)
    private boolean active;

    public IpBlock(Long id, String ipAddress, String reason, Instant blockedAt, boolean active) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.blockedAt = blockedAt;
        this.active = active;
    }

    public IpBlock() {
    }

    public static IpBlockBuilder builder() {
        return new IpBlockBuilder();
    }

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

    @Override
    public Long getAuditableId() {
        return id;
    }

    @Override
    public Long getId() {
        return id;
    }

    public static class IpBlockBuilder {
        private Long id;
        private String ipAddress;
        private String reason;
        private Instant blockedAt;
        private boolean active;

        IpBlockBuilder() {
        }

        public IpBlockBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public IpBlockBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public IpBlockBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public IpBlockBuilder blockedAt(Instant blockedAt) {
            this.blockedAt = blockedAt;
            return this;
        }

        public IpBlockBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public IpBlock build() {
            return new IpBlock(this.id, this.ipAddress, this.reason, this.blockedAt, this.active);
        }

        public String toString() {
            return "IpBlock.IpBlockBuilder(id=" + this.id + ", ipAddress=" + this.ipAddress + ", reason=" + this.reason + ", blockedAt=" + this.blockedAt + ", active=" + this.active + ")";
        }
    }
}