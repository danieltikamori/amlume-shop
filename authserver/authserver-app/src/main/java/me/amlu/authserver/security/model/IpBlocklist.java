/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.model;

import jakarta.persistence.*;
import me.amlu.authserver.model.BaseEntity;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;


@Entity
@Table(name = "ip_blocklist", indexes = {
        @Index(name = "idx_ip_block_address", columnList = "ip_address")
})
@Cacheable
public class IpBlocklist extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    @Column(name = "blocked_until", nullable = true)
    private Instant blockedUntil;

    @Column(name = "active", nullable = false)
    private boolean active;

    public IpBlocklist(Long id, String ipAddress, String reason, Instant blockedAt, boolean active) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.blockedAt = blockedAt;
        this.active = active;
    }

    public IpBlocklist() {
    }

    public IpBlocklist(Long id, String ipAddress, String reason, Instant blockedAt, Instant blockedUntil, boolean active) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.blockedAt = blockedAt;
        this.blockedUntil = blockedUntil;
        this.active = active;
    }

    public static IpBlockBuilder builder() {
        return new IpBlockBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpBlocklist ipBlocklist)) return false;
        return active == ipBlocklist.active && Objects.equals(id, ipBlocklist.id) && Objects.equals(ipAddress, ipBlocklist.ipAddress) && Objects.equals(reason, ipBlocklist.reason) && Objects.equals(blockedAt, ipBlocklist.blockedAt) && Objects.equals(blockedUntil, ipBlocklist.blockedUntil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ipAddress, reason, blockedAt, blockedUntil, active);
    }

    public Long getAuditableId() {
        return this.id;
    }

    public Long getId() {
        return id;
    }

    public static class IpBlockBuilder {
        private Long id;
        private String ipAddress;
        private String reason;
        private Instant blockedAt;
        private Instant blockedUntil;
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

        public IpBlockBuilder blockedUntil(Instant blockedUntil) {
            this.blockedUntil = blockedUntil;
            return this;
        }

        public IpBlockBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public IpBlocklist build() {
            return new IpBlocklist(this.id, this.ipAddress, this.reason, this.blockedAt, this.blockedUntil, this.active);
        }

        public String toString() {
            return "IpBlock.IpBlockBuilder(id=" + this.id +
                    ", ipAddress=" + this.ipAddress +
                    ", reason=" + this.reason +
                    ", blockedAt=" + this.blockedAt +
                    ", blockedUntil=" + this.blockedUntil +
                    ", active=" + this.active +
                    ")";
        }
    }
}
