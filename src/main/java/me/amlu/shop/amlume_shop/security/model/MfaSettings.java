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

import java.util.Objects;

@Entity
@Table(name = "mfa_settings")
public class MfaSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mfaSettingsId;

    @Column(name = "mfa_enforced")
    private boolean mfaEnforced;

    public MfaSettings(Long mfaSettingsId, boolean mfaEnforced) {
        this.mfaSettingsId = mfaSettingsId;
        this.mfaEnforced = mfaEnforced;
    }

    public MfaSettings() {
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        if (!(o instanceof MfaSettings that)) return false;
        return getMfaSettingsId() != null && Objects.equals(getMfaSettingsId(), that.getMfaSettingsId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    public Long getMfaSettingsId() {
        return this.mfaSettingsId;
    }

    public boolean isMfaEnforced() {
        return this.mfaEnforced;
    }

    public void setMfaSettingsId(Long mfaSettingsId) {
        this.mfaSettingsId = mfaSettingsId;
    }

    public void setId(Long mfaSettingsId) {
        this.mfaSettingsId = mfaSettingsId;
    }

    public void setMfaEnforced(boolean mfaEnforced) {
        this.mfaEnforced = mfaEnforced;
    }

    public String toString() {
        return "MfaSettings(mfaSettingsId=" + this.getMfaSettingsId() + ", mfaEnforced=" + this.isMfaEnforced() + ")";
    }
}