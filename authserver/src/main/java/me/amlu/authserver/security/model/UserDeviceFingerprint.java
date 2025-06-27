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
import jakarta.validation.constraints.NotBlank;
import me.amlu.authserver.model.AbstractAuditableEntity;
import me.amlu.authserver.user.model.User;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
public class UserDeviceFingerprint extends AbstractAuditableEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_device_fingerprint_id")
    private Long userDeviceFingerprintId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    // @ToString.Exclude // Add if using Lombok's @ToString to prevent infinite loop
    private User user;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt = Instant.now();

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;
    @Column(name = "trusted")
    private boolean trusted;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "device_fingerprint", unique = true, nullable = false)
    private String deviceFingerprint;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "last_known_ip")
    private String lastKnownIp;

    @Column(name = "update_count", nullable = false)
    private int updateCount = 0;

    @Column(name = "location")
    private String location;

    @Column(name = "last_known_country")
    private String lastKnownCountry;

    @NotBlank
    @Column(name = "browser_info", nullable = false)
    private String browserInfo;

    @Column(name = "source")
    private String source;

    // --- Constructors ---
    public UserDeviceFingerprint() {
        // Default values are set directly on field declarations
    }

    // All-args constructor for builder
    public UserDeviceFingerprint(Long userDeviceFingerprintId, User user,
                                 Instant lastUsedAt, Instant deactivatedAt, int failedAttempts, boolean trusted,
                                 boolean active, String deviceFingerprint, String deviceName, String lastKnownIp,
                                 int updateCount, String location, String lastKnownCountry, String browserInfo, String source) {
        this.userDeviceFingerprintId = userDeviceFingerprintId;
        this.user = user;
        this.lastUsedAt = lastUsedAt;
        this.deactivatedAt = deactivatedAt;
        this.failedAttempts = failedAttempts;
        this.trusted = trusted;
        this.active = active;
        this.deviceFingerprint = deviceFingerprint;
        this.deviceName = deviceName;
        this.lastKnownIp = lastKnownIp;
        this.updateCount = updateCount;
        this.location = location;
        this.lastKnownCountry = lastKnownCountry;
        this.browserInfo = browserInfo;
        this.source = source; // Initialize new field
    }

    // --- Manual Builder ---
    // Removed $default$ methods as they are not needed for manual builder with direct field defaults.
    public static UserDeviceFingerprintBuilder builder() {
        return new UserDeviceFingerprintBuilder();
    }

    // --- Behavioral Methods ---
    public boolean isDeviceFingerprintingEnabled() {
        User userToBeChecked = getUser();
        // Ensure user is not null and not a Hibernate proxy that hasn't been initialized
        if (userToBeChecked instanceof HibernateProxy) {
            // If it's a proxy, try to unproxy it or handle the lazy loading.
            // For this specific check, it might be safer to load the user fully if not already.
            // However, if the user is already loaded in the session, this is fine.
            // For now, rely on the User entity's own isDeviceFingerprintingEnabled which handles its embedded.
            return userToBeChecked != null && userToBeChecked.isDeviceFingerprintingEnabled();
        }
        return userToBeChecked != null && userToBeChecked.isDeviceFingerprintingEnabled();
    }

    // --- Getters and Setters ---
//    @Override
    public Long getAuditableId() {
        return getUserDeviceFingerprintId();
    }

    public Long getId() {
        return getUserDeviceFingerprintId();
    }

    public Long getUserDeviceFingerprintId() {
        return this.userDeviceFingerprintId;
    }

    public User getUser() {
        return this.user;
    }

    public Instant getLastUsedAt() {
        return this.lastUsedAt;
    }

    public Instant getDeactivatedAt() {
        return this.deactivatedAt;
    }

    public int getFailedAttempts() {
        return this.failedAttempts;
    }

    public boolean isTrusted() {
        return this.trusted;
    }

    public boolean isActive() {
        return this.active;
    }

    public String getDeviceFingerprint() {
        return this.deviceFingerprint;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public String getLastKnownIp() {
        return this.lastKnownIp;
    }

    public int getUpdateCount() {
        return this.updateCount;
    }

    public String getLocation() {
        return this.location;
    }

    public String getLastKnownCountry() {
        return this.lastKnownCountry;
    }

    public String getBrowserInfo() {
        return this.browserInfo;
    }

    public String getSource() { // ADDED: Getter for source
        return this.source;
    }

    public void setUserDeviceFingerprintId(Long userDeviceFingerprintId) {
        this.userDeviceFingerprintId = userDeviceFingerprintId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public void setDeactivatedAt(Instant deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setLastKnownIp(String lastKnownIp) {
        this.lastKnownIp = lastKnownIp;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLastKnownCountry(String lastKnownCountry) {
        this.lastKnownCountry = lastKnownCountry;
    }

    public void setBrowserInfo(String browserInfo) {
        this.browserInfo = browserInfo;
    }

    public void setSource(String source) { // ADDED: Setter for source
        this.source = source;
    }

    // --- equals, hashCode, toString ---
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        if (!(o instanceof UserDeviceFingerprint userDeviceFingerprint)) return false;
        return getUserDeviceFingerprintId() != null && Objects.equals(getUserDeviceFingerprintId(), userDeviceFingerprint.getUserDeviceFingerprintId());
    }

    @Override
    public final int hashCode() {
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        return thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserDeviceFingerprint(userDeviceFingerprintId=" + this.getUserDeviceFingerprintId() +
                ", lastUsedAt=" + this.getLastUsedAt() +
                ", deactivatedAt=" + this.getDeactivatedAt() +
                ", failedAttempts=" + this.getFailedAttempts() +
                ", trusted=" + this.isTrusted() +
                ", active=" + this.isActive() +
                ", deviceFingerprint=" + (this.getDeviceFingerprint() != null ? this.getDeviceFingerprint().substring(0, Math.min(this.getDeviceFingerprint().length(), 10)) + "..." : "null") + // Truncate
                ", deviceName=" + this.getDeviceName() +
                ", lastKnownIp=" + this.getLastKnownIp() +
                ", updateCount=" + this.getUpdateCount() +
                ", location=" + this.getLocation() +
                ", lastKnownCountry=" + this.getLastKnownCountry() +
                ", browserInfo=" + this.getBrowserInfo() +
                ", source=" + this.getSource() + // Include new field
                ")";
    }

    // --- Manual Builder Class ---
    public static class UserDeviceFingerprintBuilder {
        private Long userDeviceFingerprintId;
        private User user;
        private Instant lastUsedAt;
        private Instant deactivatedAt;
        private int failedAttempts;
        private boolean trusted;
        private boolean active;
        private String deviceFingerprint;
        private String deviceName;
        private String lastKnownIp;
        private int updateCount;
        private String location;
        private String lastKnownCountry;
        private String browserInfo;
        private String source;

        UserDeviceFingerprintBuilder() {
            // Set default values directly in the builder constructor
            this.failedAttempts = 0;
            this.active = true;
            this.updateCount = 0;
            this.lastUsedAt = Instant.now(); // Default for new fingerprints
        }

        public UserDeviceFingerprintBuilder userDeviceFingerprintId(Long userDeviceFingerprintId) {
            this.userDeviceFingerprintId = userDeviceFingerprintId;
            return this;
        }

        public UserDeviceFingerprintBuilder user(User user) {
            this.user = user;
            return this;
        }

        public UserDeviceFingerprintBuilder lastUsedAt(Instant lastUsedAt) {
            this.lastUsedAt = lastUsedAt;
            return this;
        }

        public UserDeviceFingerprintBuilder deactivatedAt(Instant deactivatedAt) {
            this.deactivatedAt = deactivatedAt;
            return this;
        }

        public UserDeviceFingerprintBuilder failedAttempts(int failedAttempts) {
            this.failedAttempts = failedAttempts; // Direct assignment
            return this;
        }

        public UserDeviceFingerprintBuilder trusted(boolean trusted) {
            this.trusted = trusted;
            return this;
        }

        public UserDeviceFingerprintBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public UserDeviceFingerprintBuilder deviceFingerprint(String deviceFingerprint) {
            this.deviceFingerprint = deviceFingerprint;
            return this;
        }

        public UserDeviceFingerprintBuilder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public UserDeviceFingerprintBuilder lastKnownIp(String lastKnownIp) {
            this.lastKnownIp = lastKnownIp;
            return this;
        }

        public UserDeviceFingerprintBuilder updateCount(int updateCount) {
            this.updateCount = updateCount; // Direct assignment
            return this;
        }

        public UserDeviceFingerprintBuilder location(String location) {
            this.location = location;
            return this;
        }

        public UserDeviceFingerprintBuilder lastKnownCountry(String lastKnownCountry) {
            this.lastKnownCountry = lastKnownCountry;
            return this;
        }

        public UserDeviceFingerprintBuilder browserInfo(String browserInfo) {
            this.browserInfo = browserInfo;
            return this;
        }

        public UserDeviceFingerprintBuilder source(String source) {
            this.source = source;
            return this;
        }

        public UserDeviceFingerprint build() {
            // All fields are directly assigned or have defaults in the constructor.
            // No need for $default$ calls or $set checks.
            return new UserDeviceFingerprint(this.userDeviceFingerprintId, this.user,
                    this.lastUsedAt, this.deactivatedAt, this.failedAttempts, this.trusted, this.active,
                    this.deviceFingerprint, this.deviceName, this.lastKnownIp, this.updateCount, this.location,
                    this.lastKnownCountry, this.browserInfo, this.source); // Pass new field
        }

        @Override
        public String toString() {
            return "UserDeviceFingerprint.UserDeviceFingerprintBuilder(" +
                    "userDeviceFingerprintId=" + this.userDeviceFingerprintId +
                    ", user=" + (this.user != null ? this.user.getId() : "null") + // Avoid N+1
                    ", lastUsedAt=" + this.lastUsedAt +
                    ", deactivatedAt=" + this.deactivatedAt +
                    ", failedAttempts=" + this.failedAttempts +
                    ", trusted=" + this.trusted +
                    ", active=" + this.active +
                    ", deviceFingerprint=" + (this.deviceFingerprint != null ? this.deviceFingerprint.substring(0, Math.min(this.deviceFingerprint.length(), 10)) + "..." : "null") +
                    ", deviceName=" + this.deviceName +
                    ", lastKnownIp=" + this.lastKnownIp +
                    ", updateCount=" + this.updateCount +
                    ", location=" + this.location +
                    ", lastKnownCountry=" + this.lastKnownCountry +
                    ", browserInfo=" + this.browserInfo +
                    ", source=" + this.source +
                    ")";
        }
    }
}
