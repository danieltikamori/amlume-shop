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
import jakarta.validation.constraints.NotBlank;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.user_management.User;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;

@Entity
public class UserDeviceFingerprint extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_device_fingerprint_id")
    private Long userDeviceFingerprintId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    //    @ToString.Exclude
    private User user;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt = Instant.now();

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;
    @Column(name = "trusted")
    private boolean trusted;

    @NotBlank
    @Column(name = "active")
    private boolean active = true;

    @Column(name = "device_fingerprint", unique = true, nullable = false)
    private String deviceFingerprint;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "last_known_ip")
    private String lastKnownIp;

    @NotBlank
    @Column(name = "update_count")
    private int updateCount = 0;

    @Column(name = "location")
    private String location;

    @Column(name = "last_known_country") private String lastKnownCountry;
    @NotBlank
    @Column(name = "browser_info", nullable = false)
    private String browserInfo;

    public UserDeviceFingerprint() {
    }

    public UserDeviceFingerprint(Long userDeviceFingerprintId, User user, String accessToken, String refreshToken, Instant lastUsedAt, Instant deactivatedAt, int failedAttempts, boolean trusted, boolean active, String deviceFingerprint, String deviceName, String lastKnownIp, int updateCount, String location, String lastKnownCountry, String browserInfo) {
        this.userDeviceFingerprintId = userDeviceFingerprintId;
        this.user = user;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
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
    }

    private static int $default$failedAttempts() {
        return 0;
    }

    private static boolean $default$active() {
        return true;
    }

    @NotBlank
    private static int $default$updateCount() {
        return 0;
    }

    public static UserDeviceFingerprintBuilder builder() {
        return new UserDeviceFingerprintBuilder();
    }


    public boolean isDeviceFingerprintingEnabled() {
        User userToBeChecked = getUser();
        return userToBeChecked != null && userToBeChecked.isDeviceFingerprintingEnabled();
    }

    @Override
    public Long getAuditableId() {
        return getUserDeviceFingerprintId();
    }

    @Override
    public Long getId() {
        return getUserDeviceFingerprintId();
    }

    public Long getUserDeviceFingerprintId() {
        return this.userDeviceFingerprintId;
    }

    public User getUser() {
        return this.user;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
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

    public @NotBlank String getDeviceName() {
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

    public void setUserDeviceFingerprintId(Long userDeviceFingerprintId) {
        this.userDeviceFingerprintId = userDeviceFingerprintId;
    }

    public void setUser(@NotBlank User user) {
        this.user = user;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
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

    public void setUpdateCount(@NotBlank int updateCount) {
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

    public String toString() {
        return "UserDeviceFingerprint(userDeviceFingerprintId=" + this.getUserDeviceFingerprintId() + ", accessToken=" + this.getAccessToken() + ", refreshToken=" + this.getRefreshToken() + ", lastUsedAt=" + this.getLastUsedAt() + ", deactivatedAt=" + this.getDeactivatedAt() + ", failedAttempts=" + this.getFailedAttempts() + ", trusted=" + this.isTrusted() + ", active=" + this.isActive() + ", deviceFingerprint=" + this.getDeviceFingerprint() + ", deviceName=" + this.getDeviceName() + ", lastKnownIp=" + this.getLastKnownIp() + ", updateCount=" + this.getUpdateCount() + ", location=" + this.getLocation() + ", lastKnownCountry=" + this.getLastKnownCountry() + ", browserInfo=" + this.getBrowserInfo() + ")";
    }

    public Object getSource() {
        return null;
    }

//    @NotBlank
//    @Column(name = "device_type")
//    private String deviceType;
//
//    @NotBlank
//    @Column(name = "device_os")
//    private String deviceOs;
//
//    @NotBlank
//    @Column(name = "device_os_version")
//    private String deviceOsVersion;
//
//    @NotBlank
//    @Column(name = "device_browser")
//    private String deviceBrowser;
//
//    @NotBlank
//    @Column(name = "device_browser_version")
//    private String deviceBrowserVersion;
//
//    @NotBlank
//    @Column(name = "device_language")
//    private String deviceLanguage;
//
//    @NotBlank
//    @Column(name = "device_timezone")
//    private String deviceTimezone;
//
//    @NotBlank
//    @Column(name = "device_ip")
//    private String deviceIp;
//
//    @NotBlank
//    @Column(name = "device_ip_location")
//    private String deviceIpLocation;
//
//    @NotBlank
//    @Column(name = "device_ip_country")
//    private String deviceIpCountry;
//
//    @NotBlank
//    @Column(name = "device_ip_region")
//    private String deviceIpRegion;
//
//    @NotBlank
//    @Column(name = "device_ip_city")
//    private String deviceIpCity;
//
//    @NotBlank
//    @Column(name = "device_ip_zip")
//    private String deviceIpZip;
//
//    @NotBlank
//    @Column(name = "device_ip_lat")
//    private String deviceIpLat;
//
//    @NotBlank
//    @Column(name = "device_ip_lon")
//    private String deviceIpLon;
//
//    @NotBlank
//    @Column(name = "device_ip_timezone")
//    private String deviceIpTimezone;
//
//    @NotBlank
//    @Column(name = "device_ip_asn")
//    private String deviceIpAsn;
//
//    @NotBlank
//    @Column(name = "device_ip_org")
//    private String deviceIpOrg;
//
//    @NotBlank
//    @Column(name = "device_ip_isp")
//    private String deviceIpIsp;
//
//    @NotBlank
//    @Column(name = "device_ip_user_agent")
//    private String deviceIpUserAgent;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer")
//    private String deviceIpReferrer;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_domain")
//    private String deviceIpReferrerDomain;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_path")
//    private String deviceIpReferrerPath;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_port")
//    private String deviceIpReferrerPort;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_protocol")
//    private String deviceIpReferrerProtocol;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_query")
//    private String deviceIpReferrerQuery;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_fragment")
//    private String deviceIpReferrerFragment;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_hash")
//    private String deviceIpReferrerHash;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_host")
//    private String deviceIpReferrerHost;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_port")
//    private String deviceIpReferrerPort;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_scheme")
//    private String deviceIpReferrerScheme;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_user")
//    private String deviceIpReferrerUser;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_password")
//    private String deviceIpReferrerPassword;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_pathname")
//    private String deviceIpReferrerPathname;
//
//    @NotBlank
//    @Column(name = "device_ip_referrer_search")
//    private String deviceIpReferrerSearch;

    public static class UserDeviceFingerprintBuilder {
        private boolean active;
        private Long userDeviceFingerprintId;
        private User user;
        private String accessToken;
        private String refreshToken;
        private Instant lastUsedAt;
        private Instant deactivatedAt;
        private @NotBlank int failedAttempts$value;
        private boolean failedAttempts$set;
        private @NotBlank boolean trusted;
        private @NotBlank String deviceFingerprint;
        private @NotBlank String deviceName;
        private String lastKnownIp;
        private @NotBlank int updateCount$value;
        private boolean updateCount$set;
        private String location;
        private String lastKnownCountry;
        private @NotBlank String browserInfo;

        UserDeviceFingerprintBuilder() {
        }


        public UserDeviceFingerprintBuilder isActive(boolean active) {
            this.active = active;
            return this;
        }

        public UserDeviceFingerprintBuilder userDeviceFingerprintId(Long userDeviceFingerprintId) {
            this.userDeviceFingerprintId = userDeviceFingerprintId;
            return this;
        }

        public UserDeviceFingerprintBuilder user(User user) {
            this.user = user;
            return this;
        }

        public UserDeviceFingerprintBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public UserDeviceFingerprintBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
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
            this.failedAttempts$value = failedAttempts;
            this.failedAttempts$set = true;
            return this;
        }

        public UserDeviceFingerprintBuilder trusted(@NotBlank boolean trusted) {
            this.trusted = trusted;
            return this;
        }

        public UserDeviceFingerprintBuilder deviceFingerprint(@NotBlank String deviceFingerprint) {
            this.deviceFingerprint = deviceFingerprint;
            return this;
        }

        public UserDeviceFingerprintBuilder deviceName(@NotBlank String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public UserDeviceFingerprintBuilder lastKnownIp(String lastKnownIp) {
            this.lastKnownIp = lastKnownIp;
            return this;
        }

        public UserDeviceFingerprintBuilder updateCount(int updateCount) {
            this.updateCount$value = updateCount;
            this.updateCount$set = true;
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

        public UserDeviceFingerprintBuilder browserInfo(@NotBlank String browserInfo) {
            this.browserInfo = browserInfo;
            return this;
        }

        public UserDeviceFingerprint build() {
            int failedAttempts$value = this.failedAttempts$value;
            if (!this.failedAttempts$set) {
                failedAttempts$value = UserDeviceFingerprint.$default$failedAttempts();
            }
            boolean active$value = this.active;

            active$value = UserDeviceFingerprint.$default$active();

            int updateCount$value = this.updateCount$value;
            if (!this.updateCount$set) {
                updateCount$value = UserDeviceFingerprint.$default$updateCount();
            }
            return new UserDeviceFingerprint(this.userDeviceFingerprintId, this.user, this.accessToken, this.refreshToken, this.lastUsedAt, this.deactivatedAt, failedAttempts$value, this.trusted, active$value, this.deviceFingerprint, this.deviceName, this.lastKnownIp, updateCount$value, this.location, this.lastKnownCountry, this.browserInfo);
        }

        public String toString() {
            return "UserDeviceFingerprint.UserDeviceFingerprintBuilder(active=" + this.active + ", userDeviceFingerprintId=" + this.userDeviceFingerprintId + ", user=" + this.user + ", accessToken=" + this.accessToken + ", refreshToken=" + this.refreshToken + ", lastUsedAt=" + this.lastUsedAt + ", deactivatedAt=" + this.deactivatedAt + ", failedAttempts$value=" + this.failedAttempts$value + ", failedAttempts$set=" + this.failedAttempts$set + ", trusted=" + this.trusted + ", deviceFingerprint=" + this.deviceFingerprint + ", deviceName=" + this.deviceName + ", lastKnownIp=" + this.lastKnownIp + ", updateCount$value=" + this.updateCount$value + ", updateCount$set=" + this.updateCount$set + ", location=" + this.location + ", lastKnownCountry=" + this.lastKnownCountry + ", browserInfo=" + this.browserInfo + ", failedAttempts$value=" + this.failedAttempts$value + ", updateCount$value=" + this.updateCount$value + ")";
        }
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
        if (!(o instanceof UserDeviceFingerprint userDeviceFingerprint)) return false;
        return getUserDeviceFingerprintId() != null && Objects.equals(getUserDeviceFingerprintId(), userDeviceFingerprint.getUserDeviceFingerprintId());
    }

    @Override
    public final int hashCode() {
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        return thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
