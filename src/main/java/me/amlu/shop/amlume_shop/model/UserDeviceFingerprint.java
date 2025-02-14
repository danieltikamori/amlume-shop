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
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@Entity
public class UserDeviceFingerprint extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_device_fingerprint_id")
    private Long userDeviceFingerprintId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotBlank
    @ToString.Exclude
//    @ToString.Exclude
    private User user;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @NotBlank
    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt = Instant.now();

    @NotBlank
    @Column(name = "trusted")
    private boolean trusted;

    @NotBlank
    @Column(name = "active")
    private boolean active;


    @NotBlank
    @Column(name = "device_fingerprint", unique = true, nullable = false)
    private String deviceFingerprint;

    @NotBlank
    @Column(name = "device_name")
    private String deviceName;

//    @PrePersist
//    protected void onCreate() {
//        createdAt = Instant.now();
//    }

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