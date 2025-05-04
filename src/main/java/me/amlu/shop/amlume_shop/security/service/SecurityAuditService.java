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

import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.security.model.SecurityEvent;
import me.amlu.shop.amlume_shop.security.model.SecurityEventType;
import me.amlu.shop.amlume_shop.order_management.Order;
import me.amlu.shop.amlume_shop.product_management.Product;
import me.amlu.shop.amlume_shop.security.repository.SecurityEventRepository;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final SecurityEventRepository securityEventRepository;

    public SecurityAuditService(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    public void logSuccessfulRegistration(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.REGISTRATION_SUCCESSFUL)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.info("Successful registration for user: {}", username);
    }

    public void logFailedLogin(String username, String ipAddress, String reason) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.FAILED_LOGIN)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .details("Failed login attempt: " + reason)
                .build();

        securityEventRepository.save(event);
        log.warn("Failed login attempt for user: {} from IP: {}", username, ipAddress);
    }

    public void logAccountLocked(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.ACCOUNT_LOCKED)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.warn("Account locked for user: {}", username);
    }

    public void logSuccessfulLogin(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.SUCCESSFUL_LOGIN)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.info("Successful login for user: {}", username);
    }

    public void logLogout(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.LOGOUT)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.info("User logged out: {}", username);
    }

    public void logLogout(String username, String reason) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.LOGOUT)
                .username(username)
                .details(Map.of("reason", reason).toString())
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.info("Successful logout for user: {}", username);
    }

    public void logPasswordChange(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.PASSWORD_CHANGED)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.info("Password changed for user: {}", username);
    }

    public void logMfaChallengeInitiated(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.MFA_CHALLENGE_INITIATED)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.info("MFA challenge initiated for user: {}", username);
    }

    public void logMfaChallengeFailed(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.MFA_CHALLENGE_FAILED)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.warn("MFA challenge failed for user: {}", username);
    }

    public void logAccountUnlocked(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.ACCOUNT_UNLOCKED)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.info("Account unlocked for user: {}", username);
    }

    public void logAccessDeniedLockedAccount(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.ACCESS_DENIED_LOCKED)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.warn("Access denied for locked account for user: {}", username);
    }

    public void logMfaVerificationFailed(String userId, String username, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.MFA_VERIFICATION_FAILED)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();

        securityEventRepository.save(event);
        log.warn("MFA verification failed for user: {}", username);
    }

    public void logRoleAssignment(String username, Set<UserRole> roles, Object resource) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.ROLE_ASSIGNMENT)
                .username(username)
                .timestamp(Instant.now())
                .details(Map.of(
                        "roles", roles.stream()
                                .map(UserRole::getRoleName)
                                .collect(Collectors.toSet()),
                        "resource", resource.getClass().getSimpleName(),
                        "resourceId", Objects.requireNonNull(getResourceId(resource))
                ).toString())
                .build();

        securityEventRepository.save(event);
        log.info("Audit log created for role assignment: {}", event);
    }

    public void logFailedAttempt(String username, String reason) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.ROLE_ASSIGNMENT_FAILED)
                .username(username)
                .timestamp(Instant.now())
                .details(Map.of("reason", reason).toString())
                .build();

        securityEventRepository.save(event);
        log.warn("Failed role assignment attempt logged: {}", event);
    }

    private String getResourceId(Object resource) {
        return switch (resource) {
            case Product product -> String.valueOf(product.getProductId());
            case Order order -> String.valueOf(order.getOrderId());
            case Category category -> String.valueOf(category.getCategoryId());
            case null, default -> null; // or throw an exception if the resource ID is not found

        };
    }

    public void logCacheCleared(String message) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.CACHE_CLEARED)
                .username(getCurrentUsername())
                .timestamp(Instant.now())
                .details(Map.of("message", message).toString())
                .build();

        securityEventRepository.save(event);
        log.info("Cache clear audit log created: {}", event);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
