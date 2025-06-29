/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import me.amlu.authserver.common.Roles;
import me.amlu.authserver.exceptions.ForbiddenException;
import me.amlu.authserver.exceptions.InvalidCredentialsException;
import me.amlu.authserver.exceptions.UserNotFoundException;

import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.vo.HashedPassword;
import me.amlu.authserver.user.service.UserLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for secure password management operations.
 */
@Service
public class PasswordService {
    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    private final PasswordEncoder passwordEncoder;
    private final UserLookupService userLookupService;
    private final RoleHierarchyService roleHierarchyService;

    public PasswordService(PasswordEncoder passwordEncoder,
                           UserLookupService userLookupService,
                           RoleHierarchyService roleHierarchyService) {
        this.passwordEncoder = passwordEncoder;
        this.userLookupService = userLookupService;
        this.roleHierarchyService = roleHierarchyService;
    }

    /**
     * Changes a user's password with current user authentication.
     * The current user must either be the target user or have sufficient privileges.
     *
     * @param authentication  Current user's authentication
     * @param userId          ID of the user whose password is being changed
     * @param currentPassword Current password (required for self-change, optional for admins)
     * @param newPassword     New password
     * @throws UserNotFoundException       If the target user doesn't exist
     * @throws InvalidCredentialsException If the current password is incorrect
     * @throws ForbiddenException          If the current user lacks permission
     */
    @Transactional
    public void changePassword(Authentication authentication, Long userId,
                               String currentPassword, String newPassword) {
        // Get current authenticated user
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (currentUser == null) {
            throw new ForbiddenException("Authentication required");
        }

        // Get target user
        User targetUser = getUserById(userId);

        // Check permissions
        boolean isSelfChange = currentUser.getId().equals(targetUser.getId());
        boolean isAdmin = roleHierarchyService.hasAnyRole(currentUser, Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ROOT);

        // Self-change requires current password validation
        if (isSelfChange) {
            validateCurrentPassword(targetUser, currentPassword);
        }
        // Admin change doesn't require current password but needs admin role
        else if (!isAdmin) {
            throw new ForbiddenException("Insufficient privileges to change another user's password");
        }
        // Admin changing another user's password - check role hierarchy
        else if (!roleHierarchyService.canManage(currentUser, targetUser)) {
            throw new ForbiddenException("Cannot change password for users with equal or higher privileges");
        }

        // Change password
        updatePassword(targetUser, newPassword);
        log.info("Password changed for user ID: {} by user ID: {}", targetUser.getId(), currentUser.getId());
    }

    /**
     * Administrative password reset without requiring the current password.
     * Requires admin privileges.
     *
     * @param authentication Admin's authentication
     * @param userId         ID of the user whose password is being reset
     * @param newPassword    New password
     * @throws UserNotFoundException If the target user doesn't exist
     * @throws ForbiddenException    If the current user lacks admin privileges
     */
    @Transactional
    public void resetPassword(Authentication authentication, Long userId, String newPassword) {
        // Get current authenticated user
        User adminUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (adminUser == null) {
            throw new ForbiddenException("Authentication required");
        }

        // Verify admin privileges
        if (!roleHierarchyService.hasAnyRole(adminUser, Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ROOT)) {
            throw new ForbiddenException("Admin privileges required for password reset");
        }

        User targetUser = getUserById(userId);

        // Check if admin can manage this user
        if (!roleHierarchyService.canManage(adminUser, targetUser)) {
            throw new ForbiddenException("Cannot reset password for users with equal or higher privileges");
        }

        // Reset password
        updatePassword(targetUser, newPassword);
        log.info("Password reset for user ID: {} by admin ID: {}", targetUser.getId(), adminUser.getId());
    }

    /**
     * Self-service password change.
     *
     * @param authentication  Current user's authentication
     * @param currentPassword Current password
     * @param newPassword     New password
     * @throws InvalidCredentialsException If the current password is incorrect
     */
    @Transactional
    public void changeOwnPassword(Authentication authentication, String currentPassword, String newPassword) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (currentUser == null) {
            throw new ForbiddenException("Authentication required");
        }

        validateCurrentPassword(currentUser, currentPassword);
        updatePassword(currentUser, newPassword);
        log.info("User ID: {} changed their own password", currentUser.getId());
    }

    /**
     * Force change password for a user who has forgotten their password.
     * This should only be called after proper identity verification (e.g., email verification).
     *
     * @param userId      ID of the user whose password is being changed
     * @param newPassword New password
     * @throws UserNotFoundException If the user doesn't exist
     */
    @Transactional
    public void forceChangePassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        updatePassword(user, newPassword);
        log.info("Password force changed for user ID: {}", userId);
    }

    // Helper methods

    private User getUserById(Long userId) {
        return userLookupService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    private void validateCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
    }

    private void updatePassword(User user, String newPassword) {
        HashedPassword hashedPassword = new HashedPassword(passwordEncoder.encode(newPassword));
        user.changePassword(hashedPassword);
    }
}
