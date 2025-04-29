/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import me.amlu.shop.amlume_shop.model.UserDeviceFingerprint;
import me.amlu.shop.amlume_shop.user_management.User;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditLogger {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AuditLogger.class);

    public void logDeviceAccess(String userId, String deviceFingerprint, String action) {
        log.info("Security event: {} - User: {} - Device: {} - Timestamp: {}",
                action, userId, deviceFingerprint, Instant.now());
        // You might want to save this to a database or send to a security monitoring system
    }

    public void logSecurityEvent(String action, String userId, String fingerprint) {
        log.info("Important Security event: {} - User: {} - Device: {} - Timestamp: {}",
                action, userId, fingerprint, Instant.now());
    }

    public void logSecurityEvent(String action, String userId, String fingerprint, String clientIp) {
        log.info("Important Security event: {} - User: {} - Device: {} - Timestamp: {} - Client IP: {}",
                action, userId, fingerprint, Instant.now(), clientIp);
    }

    public void logDeviceCreation(User user, UserDeviceFingerprint fingerprint) {
        log.info("Device creation: User: {} - Device: {} - Timestamp: {}",
                user.getUserId(), fingerprint.getUserDeviceFingerprintId(), Instant.now());
    }

    public void logDeviceDeletion(User user, UserDeviceFingerprint fingerprint) {
        log.info("Device deletion: User: {} - Device: {} - Timestamp: {}",
                user.getUserId(), fingerprint.getUserDeviceFingerprintId(), Instant.now());
    }

    public void logFailedValidation(String userId, String fingerprintId, String clientIp) {
        log.warn("Failed validation: User: {} - Device: {} - Client IP: {} - Timestamp: {}",
                userId, fingerprintId, clientIp, Instant.now());
    }

    public void logDeviceValidation(User user, UserDeviceFingerprint fingerprint, boolean b) {
        log.info("Device validation: User: {} - Device: {} - Result: {} - Timestamp: {}",
                user.getUserId(), fingerprint.getUserDeviceFingerprintId(), b, Instant.now());
    }

    public void logFingerprintUpdate(Long userId, String clientIp) {
        log.info("Fingerprint update: User: {} - Client IP: {} - Timestamp: {}",
                userId, clientIp, Instant.now());
    }
}
