/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

// /home/daniel/code/src/github.com/danieltikamori/amlume-shop/authserver/src/main/java/me/amlu/authserver/security/audit/SecurityAuditService.java
package me.amlu.authserver.security.audit;

import jakarta.servlet.http.HttpServletRequest;
import me.amlu.authserver.security.model.UserDeviceFingerprint;
import me.amlu.authserver.user.model.User;
import net.logstash.logback.marker.Markers; // Requires logstash-logback-encoder dependency
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SecurityAuditService {

    // Dedicated logger for security audit events
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("SECURITY_AUDIT");
    // Standard logger for internal service messages
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    /**
     * Logs a generic security audit event in a structured format.
     * This is the central method that all other specific audit methods should call.
     *
     * @param eventType         A descriptive type for the event (e.g., "AUTHENTICATION", "DEVICE_MANAGEMENT").
     * @param action            The specific action performed (e.g., "LOGIN_SUCCESS", "PASSWORD_CHANGE", "DEVICE_REVOKE").
     * @param userId            The ID of the user involved (can be null if not applicable or unknown).
     * @param targetId          The ID of the resource affected (e.g., device fingerprint ID, other user ID).
     * @param clientIp          The IP address from which the action originated.
     * @param outcome           The outcome of the action ("SUCCESS", "FAILURE", "DENIED").
     * @param additionalDetails A map for any other context-specific details.
     */
    public void logAudit(String eventType, String action, String userId, String targetId, String clientIp, String outcome, Map<String, Object> additionalDetails) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", Instant.now().toString()); // ISO 8601 format
        event.put("event_type", eventType);
        event.put("action", action);
        event.put("outcome", outcome);
        event.put("user_id", userId);
        event.put("target_id", targetId);
        event.put("client_ip", clientIp);
        event.put("actor", getActorFromSecurityContext()); // Who initiated the action
        event.put("session_id", getSessionIdFromRequest()); // Add session ID for traceability
        // Add request ID/correlation ID if available (e.g., from MDC)
        // event.put("request_id", MDC.get("X-B3-TraceId"));

        if (additionalDetails != null) {
            event.putAll(additionalDetails);
        }

        // Log the structured event using Markers for JSON output
        AUDIT_LOGGER.info(Markers.appendEntries(event), "Security Audit Event: {} - {}", eventType, action);
        log.debug("Audit event logged: Type={}, Action={}, User={}", eventType, action, userId);
    }

    // --- Specific Audit Methods (calling the generic logAudit) ---

    public void logAuthSuccess(String username, String clientIp) {
        logAudit("AUTHENTICATION", "LOGIN", username, username, clientIp, "SUCCESS", null);
    }

    public void logAuthFailure(String username, String clientIp, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        logAudit("AUTHENTICATION", "LOGIN", username, username, clientIp, "FAILURE", details);
    }

    public void logAccessDenied(String username, String resource, String clientIp, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("resource", resource);
        details.put("reason", reason);
        logAudit("AUTHORIZATION", "ACCESS_DENIED", username, resource, clientIp, "DENIED", details);
    }

    public void logDeviceAccess(String userId, String deviceFingerprint, String action, String clientIp) {
        Map<String, Object> details = new HashMap<>();
        details.put("device_fingerprint", deviceFingerprint);
        logAudit("DEVICE_MANAGEMENT", action, userId, deviceFingerprint, clientIp, "SUCCESS", details);
    }

    // Renamed 'fingerprint' to 'targetId' for clarity
    public void logSecurityEvent(String action, String userId, String targetId, String clientIp) {
        Map<String, Object> details = new HashMap<>();
        if (targetId != null) {
            details.put("target_id_details", targetId);
        }
        logAudit("GENERIC_SECURITY_EVENT", action, userId, targetId, clientIp, "INFO", details);
    }

    public void logDeviceCreation(User user, UserDeviceFingerprint fingerprint) {
        Map<String, Object> details = new HashMap<>();
        details.put("device_fingerprint_id", fingerprint.getUserDeviceFingerprintId());
        details.put("device_name", fingerprint.getDeviceName());
        details.put("browser_info", fingerprint.getBrowserInfo());
        details.put("source", fingerprint.getSource());
        details.put("trusted", fingerprint.isTrusted());
        logAudit("DEVICE_MANAGEMENT", "DEVICE_REGISTERED", String.valueOf(user.getId()), fingerprint.getDeviceFingerprint(), fingerprint.getLastKnownIp(), "SUCCESS", details);
    }

    public void logDeviceDeletion(User user, UserDeviceFingerprint fingerprint) {
        Map<String, Object> details = new HashMap<>();
        details.put("device_fingerprint_id", fingerprint.getUserDeviceFingerprintId());
        details.put("device_name", fingerprint.getDeviceName());
        logAudit("DEVICE_MANAGEMENT", "DEVICE_DELETED", String.valueOf(user.getId()), fingerprint.getDeviceFingerprint(), null, "SUCCESS", details);
    }

    public void logFailedValidation(String userId, String fingerprintId, String clientIp, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("fingerprint_id", fingerprintId);
        details.put("reason", reason);
        logAudit("VALIDATION", "FAILED_VALIDATION", userId, fingerprintId, clientIp, "FAILURE", details);
    }

    public void logDeviceValidation(User user, UserDeviceFingerprint fingerprint, boolean isValid) {
        Map<String, Object> details = new HashMap<>();
        details.put("device_fingerprint_id", fingerprint.getUserDeviceFingerprintId());
        details.put("device_name", fingerprint.getDeviceName());
        details.put("is_valid", isValid);
        logAudit("DEVICE_MANAGEMENT", "DEVICE_VALIDATION", String.valueOf(user.getId()), fingerprint.getDeviceFingerprint(), fingerprint.getLastKnownIp(), isValid ? "SUCCESS" : "FAILURE", details);
    }

    public void logFingerprintUpdate(Long userId, String clientIp) {
        logAudit("DEVICE_MANAGEMENT", "FINGERPRINT_UPDATE", String.valueOf(userId), null, clientIp, "SUCCESS", null);
    }

    public void logCacheCleared(String cacheName, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("cache_name", cacheName);
        details.put("reason", reason);
        logAudit("SYSTEM_OPERATION", "CACHE_CLEARED", getActorFromSecurityContext(), cacheName, null, "SUCCESS", details);
    }

    public void logRoleAssignment(String userId, Set<Object> roles, String assignerId) {
        Map<String, Object> details = new HashMap<>();
        details.put("assigned_roles", roles);
        details.put("assigner_id", assignerId);
        logAudit("USER_MANAGEMENT", "ROLE_ASSIGNMENT", assignerId, userId, null, "SUCCESS", details);
    }

    public void logFailedAttempt(String identifier, String ipAddress, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        logAudit("SECURITY_BREACH", "FAILED_ATTEMPT", identifier, null, ipAddress, "FAILURE", details);
    }

    public void logSuccessfulRegistration(String userId, String email, String ipAddress) {
        logAudit("USER_MANAGEMENT", "REGISTRATION", userId, email, ipAddress, "SUCCESS", null);
    }

    public void logFailedRegistration(String email, String ipAddress, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        logAudit("USER_MANAGEMENT", "REGISTRATION", null, email, ipAddress, "FAILURE", details);
    }

    public void logLogout(String userId, String username, String ipAddress) {
        logAudit("AUTHENTICATION", "LOGOUT", userId, username, ipAddress, "SUCCESS", null);
    }

    public void logDeviceDeactivation(User user, UserDeviceFingerprint fingerprint) {
        Map<String, Object> details = new HashMap<>();
        details.put("device_fingerprint_id", fingerprint.getUserDeviceFingerprintId());
        details.put("device_name", fingerprint.getDeviceName());
        logAudit("DEVICE_MANAGEMENT", "DEVICE_DEACTIVATED", String.valueOf(user.getId()), fingerprint.getDeviceFingerprint(), fingerprint.getLastKnownIp(), "SUCCESS", details);
    }

    public void logDeviceFingerprintingSettingChange(String userId, String action, String outcome) {
        logAudit("USER_SETTINGS", action, userId, null, null, outcome, null);
    }

    public void logAllDevicesRevoked(String userId, String exceptFingerprint) {
        Map<String, Object> details = new HashMap<>();
        details.put("except_fingerprint", exceptFingerprint);
        logAudit("DEVICE_MANAGEMENT", "ALL_DEVICES_REVOKED", userId, null, null, "SUCCESS", details);
    }

    // --- Helper methods to extract common context ---

    private String getActorFromSecurityContext() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonymous");
    }

    private String getSessionIdFromRequest() {
        return Optional.ofNullable(org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getSessionId)
                .orElse(null);
    }

//    private String getSessionIdFromRequest() {
//        return getCurrentHttpRequest()
//                .map(HttpServletRequest::getSession)
//                .map(jakarta.servlet.http.HttpSession::getId)
//                .orElse(null);
//    }

    private Optional<HttpServletRequest> getCurrentHttpRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest);
    }

}
