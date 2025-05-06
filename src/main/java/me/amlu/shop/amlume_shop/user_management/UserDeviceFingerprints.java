/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.security.model.UserDeviceFingerprint;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Embeddable
public class UserDeviceFingerprints implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<UserDeviceFingerprint> deviceFingerprints = new ArrayList<>();

    protected UserDeviceFingerprints() { // Required by JPA
    }

    public UserDeviceFingerprints(List<UserDeviceFingerprint> deviceFingerprints) {
        if (deviceFingerprints == null) {
            deviceFingerprints = new ArrayList<>();
        }
        this.deviceFingerprints.addAll(deviceFingerprints);
//        this.deviceFingerprints.addAll(Optional.ofNullable(deviceFingerprints).orElseGet(ArrayList::new));
    }

    public static UserDeviceFingerprintsBuilder builder() {
        return new UserDeviceFingerprintsBuilder();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprints() {
        return Collections.unmodifiableList(deviceFingerprints);
    }

    public void addDeviceFingerprint(UserDeviceFingerprint deviceFingerprint) {
        if (deviceFingerprint == null) {
            throw new IllegalArgumentException(Constants.DEVICE_FINGERPRINT_CANNOT_BE_NULL);
        }
        if (deviceFingerprints.contains(deviceFingerprint)) {
            throw new IllegalArgumentException("Device fingerprint already exists");
        }
        deviceFingerprints.add(deviceFingerprint);
    }

    public void removeDeviceFingerprint(UserDeviceFingerprint deviceFingerprint) {
        if (deviceFingerprint == null) {
            throw new IllegalArgumentException(Constants.DEVICE_FINGERPRINT_CANNOT_BE_NULL);
        }
        if (!deviceFingerprints.contains(deviceFingerprint)) {
            throw new IllegalArgumentException("Device fingerprint does not exist");
        }
        deviceFingerprints.remove(deviceFingerprint);
    }

    public int size() {
        return deviceFingerprints.size();
    }

    public boolean isEmpty() {
        return deviceFingerprints.isEmpty();
    }

    public boolean contains(UserDeviceFingerprint deviceFingerprint) {
        return deviceFingerprints.contains(deviceFingerprint);
    }

    public UserDeviceFingerprint getDeviceFingerprint(int index) {
        return deviceFingerprints.get(index);
    }

    public UserDeviceFingerprint getDeviceFingerprint(UserDeviceFingerprint deviceFingerprint) {
        if (deviceFingerprint == null) {
            throw new IllegalArgumentException(Constants.DEVICE_FINGERPRINT_CANNOT_BE_NULL);
        }
        if (!deviceFingerprints.contains(deviceFingerprint)) {
            throw new IllegalArgumentException("Device fingerprint does not exist");
        }
        return deviceFingerprints.get(deviceFingerprints.indexOf(deviceFingerprint));
    }

    public boolean containsDeviceFingerprint(UserDeviceFingerprint deviceFingerprint) {
        return deviceFingerprints.contains(deviceFingerprint);
    }

    public boolean containsDeviceFingerprint(String deviceFingerprint) {
        return deviceFingerprints.stream().anyMatch(d -> d.getDeviceFingerprint().equals(deviceFingerprint));
    }

    public UserDeviceFingerprint getDeviceFingerprint(String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint)).findFirst().orElse(null);
    }

    public List<UserDeviceFingerprint> getActiveDeviceFingerprints() {
        return deviceFingerprints.stream().filter(UserDeviceFingerprint::isActive).toList();
    }

    public List<UserDeviceFingerprint> getTrustedDeviceFingerprints() {
        return deviceFingerprints.stream().filter(UserDeviceFingerprint::isTrusted).toList();
    }

    public List<UserDeviceFingerprint> getDeactivatedDeviceFingerprints() {
        return deviceFingerprints.stream().filter(d -> d.getDeactivatedAt() != null).toList();
    }

    public List<UserDeviceFingerprint> getFailedDeviceFingerprints() {
        return deviceFingerprints.stream().filter(d -> d.getFailedAttempts() > 0).toList();
    }

    public List<UserDeviceFingerprint> getUpdatedDeviceFingerprints() {
        return deviceFingerprints.stream().filter(d -> d.getUpdateCount() > 0).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceName(String deviceName) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceName().equals(deviceName)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByLastKnownIp(String lastKnownIp) {
        return deviceFingerprints.stream().filter(d -> d.getLastKnownIp().equals(lastKnownIp)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByLocation(String location) {
        return deviceFingerprints.stream().filter(d -> d.getLocation().equals(location)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByLastKnownCountry(String lastKnownCountry) {
        return deviceFingerprints.stream().filter(d -> d.getLastKnownCountry().equals(lastKnownCountry)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByBrowserInfo(String browserInfo) {
        return deviceFingerprints.stream().filter(d -> d.getBrowserInfo().equals(browserInfo)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByAccessToken(String accessToken) {
        return deviceFingerprints.stream().filter(d -> d.getAccessToken().equals(accessToken)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByRefreshToken(String refreshToken) {
        return deviceFingerprints.stream().filter(d -> d.getRefreshToken().equals(refreshToken)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByLastUsedAt(java.time.Instant lastUsedAt) {
        return deviceFingerprints.stream().filter(d -> d.getLastUsedAt().equals(lastUsedAt)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeactivatedAt(java.time.Instant deactivatedAt) {
        return deviceFingerprints.stream().filter(d -> d.getDeactivatedAt().equals(deactivatedAt)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByFailedAttempts(int failedAttempts) {
        return deviceFingerprints.stream().filter(d -> d.getFailedAttempts() == failedAttempts).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByTrusted(boolean trusted) {
        return deviceFingerprints.stream().filter(d -> d.isTrusted() == trusted).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByActive(boolean active) {
        return deviceFingerprints.stream().filter(d -> d.isActive() == active).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByUpdateCount(int updateCount) {
        return deviceFingerprints.stream().filter(d -> d.getUpdateCount() == updateCount).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprint(String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceName(String deviceName, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceName().equals(deviceName) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByLastKnownIp(String lastKnownIp, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getLastKnownIp().equals(lastKnownIp) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByLocation(String location, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getLocation().equals(location) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByLastKnownCountry(String lastKnownCountry, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getLastKnownCountry().equals(lastKnownCountry) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByBrowserInfo(String browserInfo, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getBrowserInfo().equals(browserInfo) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByAccessToken(String accessToken, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getAccessToken().equals(accessToken) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByRefreshToken(String refreshToken, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getRefreshToken().equals(refreshToken) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByLastUsedAt(java.time.Instant lastUsedAt, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getLastUsedAt().equals(lastUsedAt) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeactivatedAt(java.time.Instant deactivatedAt, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getDeactivatedAt().equals(deactivatedAt) && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByFailedAttempts(int failedAttempts, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getFailedAttempts() == failedAttempts && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByTrusted(boolean trusted, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.isTrusted() == trusted && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByActive(boolean active, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.isActive() == active && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByUpdateCount(int updateCount, String deviceFingerprint) {
        return deviceFingerprints.stream().filter(d -> d.getUpdateCount() == updateCount && d.getDeviceFingerprint().equals(deviceFingerprint)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprint(String deviceFingerprint, String deviceName) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getDeviceFingerprint().equals(deviceName)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndLastKnownIp(String deviceFingerprint, String lastKnownIp) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getLastKnownIp().equals(lastKnownIp)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndLocation(String deviceFingerprint, String location) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getLocation().equals(location)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndLastKnownCountry(String deviceFingerprint, String lastKnownCountry) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getLastKnownCountry().equals(lastKnownCountry)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndBrowserInfo(String deviceFingerprint, String browserInfo) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getBrowserInfo().equals(browserInfo)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndAccessToken(String deviceFingerprint, String accessToken) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getAccessToken().equals(accessToken)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndRefreshToken(String deviceFingerprint, String refreshToken) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getRefreshToken().equals(refreshToken)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndLastUsedAt(String deviceFingerprint, java.time.Instant lastUsedAt) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getLastUsedAt().equals(lastUsedAt)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndDeactivatedAt(String deviceFingerprint, java.time.Instant deactivatedAt) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getDeactivatedAt().equals(deactivatedAt)).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndFailedAttempts(String deviceFingerprint, int failedAttempts) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getFailedAttempts() == failedAttempts).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndTrusted(String deviceFingerprint, boolean trusted) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.isTrusted() == trusted).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndActive(String deviceFingerprint, boolean active) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.isActive() == active).toList();
    }

    public List<UserDeviceFingerprint> getDeviceFingerprintsByDeviceFingerprintAndUpdateCount(String deviceFingerprint, int updateCount) {
        return deviceFingerprints.stream().filter(d -> d.getDeviceFingerprint().equals(deviceFingerprint) && d.getUpdateCount() == updateCount).toList();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof UserDeviceFingerprints other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$deviceFingerprints = this.getDeviceFingerprints();
        final Object other$deviceFingerprints = other.getDeviceFingerprints();
        return Objects.equals(this$deviceFingerprints, other$deviceFingerprints);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof UserDeviceFingerprints;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $deviceFingerprints = this.getDeviceFingerprints();
        result = result * PRIME + ($deviceFingerprints == null ? 43 : $deviceFingerprints.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "UserDeviceFingerprints()";
    }

    public static class UserDeviceFingerprintsBuilder {
        UserDeviceFingerprintsBuilder() {
        }

        public UserDeviceFingerprints build() {
            return new UserDeviceFingerprints();
        }

        @Override
        public String toString() {
            return "UserDeviceFingerprints.UserDeviceFingerprintsBuilder()";
        }
    }
}