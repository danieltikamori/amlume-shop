/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.controller;

import jakarta.validation.Valid;
import me.amlu.authserver.user.service.UserServiceInterface;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication; // Import for clarity
import org.springframework.security.core.context.SecurityContextHolder; // Import for clarity

@RestController
@RequestMapping("/auth-admin/users") // or /admin, although /admin is being used for Spring Admin
public class AdminUserController {

    private final UserServiceInterface userManager;

    public AdminUserController(UserServiceInterface userManager) {
        this.userManager = userManager;
    }

    // Admin managing a regular user's password
    @PreAuthorize("hasAuthority('user:admin:reset-user-password') " +
            "and @userManager.isRegularUser(#targetUserId)") // Custom check
    @PostMapping("/{targetUserId}/reset-password")
    public String adminResetUserPassword(@PathVariable Long targetUserId) {
        // Logic to reset password for a regular user
        return "Admin reset password for user: " + targetUserId;
    }

    // Super Admin managing an ADMIN's password
    @PreAuthorize("hasAuthority('user:super_admin:manage-admin-password') " +
            "and @userManager.isAdmin(#targetUserId) " +
            "and !@userManager.isSuperAdminOrRootAdmin(#targetUserId)") // Crucial: prevent escalation
    @PostMapping("/{targetUserId}/manage-admin-password")
    public String superAdminManageAdminPassword(@PathVariable Long targetUserId) {
        // Logic to manage an ADMIN's password
        return "Super Admin managed admin password for user: " + targetUserId;
    }

    // Root Admin managing a SUPER_ADMIN's password
    @PreAuthorize("hasAuthority('user:root:manage-super-admin-password') " +
            "and @userManager.isSuperAdmin(#targetUserId) " +
            "and !@userManager.isRootAdmin(#targetUserId)") // Crucial: prevent root-on-root without extra checks
    @PostMapping("/{targetUserId}/manage-super-admin-password")
    public String rootAdminManageSuperAdminPassword(@PathVariable Long targetUserId) {
        // Logic to manage a SUPER_ADMIN's password
        return "Root Admin managed super admin password for user: " + targetUserId;
    }

    // ROOT managing own password (HIGHEST SENSITIVITY)
    @PreAuthorize("hasAuthority('user:root:manage-root-admin-password') " +
            "and @userManager.isRootAdmin(#targetUserId) " +
            "and #targetUserId = authentication.principal.id")
    @PostMapping("/{targetUserId}/manage-root-admin-password")
    public String manageRootAdminPassword(@Valid @PathVariable Long targetUserId, String new_password) {
        // Logic to manage ROOT_ADMIN's password
        // !!! This operation should ideally involve further controls like MFA,
        // !!! multi-person approval, or time-based access.

        // Get the currently authenticated user
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();

        // Check if the current user is a ROOT_ADMIN
        if (currentUser.getAuthorities().stream()
                .noneMatch(ga -> ga.getAuthority().equals("ROLE_ROOT"))) {
            return "Unauthorized: Only ROOT_ADMINs can manage their own password.";
        }
        // Check if the targetUserId matches the current user's ID
        if (!currentUser.getName().equals(targetUserId.toString())) {
            return "Unauthorized: ROOT_ADMINs can only manage their own password.";
        }

        userManager.adminChangeUserPassword(targetUserId, new_password);

        return "Root Admin managed own root admin password for user: " + targetUserId;
    }
//    // ROOT managing another ROOT's password (HIGHEST SENSITIVITY)
//    @PreAuthorize("hasAuthority('user:root:manage-root-admin-password') " +
//                  "and @userManager.isRootAdmin(#targetUserId) " +
//                  "and #targetUserId != authentication.principal.id") // Can't change own password with this endpoint
//    @PostMapping("/{targetUserId}/manage-root-admin-password")
//    public String rootAdminManageRootAdminPassword(@PathVariable Long targetUserId) {
//        // Logic to manage another ROOT_ADMIN's password
//        // !!! This operation should ideally involve further controls like MFA,
//        // !!! multi-person approval, or time-based access.
//        return "Root Admin managed another root admin password for user: " + targetUserId;
//    }

}
