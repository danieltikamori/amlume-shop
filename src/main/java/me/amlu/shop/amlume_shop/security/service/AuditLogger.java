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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class AuditLogger {
    public void logDeviceAccess(String userId, String deviceFingerprint, String action) {
        log.info("Security event: {} - User: {} - Device: {} - Timestamp: {}", 
                action, userId, deviceFingerprint, Instant.now());
        // You might want to save this to a database or send to a security monitoring system
    }

    public void logSecurityEvent(String userId, String fingerprint, String action) {
        log.info("Important Security event: {} - User: {} - Device: {} - Timestamp: {}",
                action, userId, fingerprint, Instant.now());
    }
}
