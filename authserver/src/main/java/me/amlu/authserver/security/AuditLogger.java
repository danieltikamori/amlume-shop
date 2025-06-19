/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Component for security audit logging.
 * Provides standardized methods for logging security-related events.
 */
@Component
public class AuditLogger {
    private static final Logger auditLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    /**
     * Logs a security event with user information.
     *
     * @param action   Description of the security action
     * @param targetId ID of the target resource (user, etc.)
     * @param details  Additional details about the action
     */
    public void logSecurityEvent(String action, String targetId, Map<String, Object> details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null ? auth.getName() : "anonymous";

        StringBuilder message = new StringBuilder();
        message.append("SECURITY_EVENT [")
                .append(action)
                .append("] Actor: ")
                .append(actor)
                .append(", Target: ")
                .append(targetId);

        if (details != null && !details.isEmpty()) {
            message.append(", Details: ").append(details);
        }

        auditLog.info(message.toString());
    }

    /**
     * Logs a failed authentication attempt.
     *
     * @param username  The username that failed authentication
     * @param reason    The reason for the failure
     * @param ipAddress The IP address of the request
     */
    public void logAuthFailure(String username, String reason, String ipAddress) {
        auditLog.warn("AUTH_FAILURE User: {}, Reason: {}, IP: {}",
                username != null ? username : "unknown",
                reason,
                ipAddress != null ? ipAddress : "unknown");
    }

    /**
     * Logs a successful authentication.
     *
     * @param username  The authenticated username
     * @param ipAddress The IP address of the request
     */
    public void logAuthSuccess(String username, String ipAddress) {
        auditLog.info("AUTH_SUCCESS User: {}, IP: {}",
                username,
                ipAddress != null ? ipAddress : "unknown");
    }

    /**
     * Logs an access denied event.
     *
     * @param username  The username that was denied access
     * @param resource  The resource that was attempted to be accessed
     * @param ipAddress The IP address of the request
     */
    public void logAccessDenied(String username, String resource, String ipAddress) {
        auditLog.warn("ACCESS_DENIED User: {}, Resource: {}, IP: {}",
                username != null ? username : "unknown",
                resource,
                ipAddress != null ? ipAddress : "unknown");
    }
}
