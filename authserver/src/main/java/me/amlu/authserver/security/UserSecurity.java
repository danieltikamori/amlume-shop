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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security component for user-specific authorization checks.
 * Used in SpEL expressions for method security.
 */
@Component("userSecurity")
public class UserSecurity {

    /**
     * Checks if the current authenticated user is allowed to modify the specified user.
     *
     * @param userId The ID of the user being modified
     * @return true if the current user is allowed to modify the specified user
     */
    public boolean isUserAllowedToModify(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        // Admin users can modify any user
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_SUPER_ADMIN") ||
                        a.getAuthority().equals("ROLE_ROOT"))) {
            return true;
        }

        // Regular users can only modify themselves
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            try {
                Long currentUserId = Long.parseLong(
                        ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername()
                );
                return currentUserId.equals(userId);
            } catch (NumberFormatException e) {
                // If username is not a numeric ID, compare by string
                return false;
            }
        }

        return false;
    }
}
