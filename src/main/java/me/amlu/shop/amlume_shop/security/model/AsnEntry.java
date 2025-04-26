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
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "asn_entries", indexes = {
        @Index(name = "idx_asn_entries_last_updated", columnList = "last_updated"),
        @Index(name = "idx_asn_entries_asn", columnList = "asn")
})
public class AsnEntry {
    @Id
    private String ip;

    @Column(nullable = false)
    private String asn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public AsnEntry(String ip, String asn) {
        this.ip = ip;
        this.asn = asn;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public AsnEntry() {
    }

    public AsnEntry(String ip, String asn, LocalDateTime createdAt, LocalDateTime lastUpdated) {
        this.ip = ip;
        this.asn = asn;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        if (!(o instanceof AsnEntry asnEntry)) return false;
        return getIp() != null && Objects.equals(getIp(), asnEntry.getIp());
    }

    @Override
    public final int hashCode() {
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        return thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    public String getIp() {
        return this.ip;
    }

    public String getAsn() {
        return this.asn;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return this.lastUpdated;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setAsn(String asn) {
        this.asn = asn;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String toString() {
        return "AsnEntry(ip=" + this.getIp() + ", asn=" + this.getAsn() + ", createdAt=" + this.getCreatedAt() + ", lastUpdated=" + this.getLastUpdated() + ")";
    }
}
