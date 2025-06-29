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

import me.amlu.authserver.user.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/*
 * Security component for user-specific authorization checks.
 * Used in SpEL expressions for method security (e.g., @PreAuthorize).
 */

/**
 * Component for performing user-specific security checks, primarily used within Spring Security's
 * expression-based access control (e.g., {@code @PreAuthorize}, {@code @PostAuthorize}).
 */
@Component("userSecurity")
public class UserSecurity {

    /**
     * Checks if the current authenticated user is allowed to modify the specified user's data.
     *
     * @param userId The ID of the user being modified.
     * @return {@code true} if the current authenticated user is an administrator (ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT)
     *         or if the current user is attempting to modify their own data; {@code false} otherwise.
     * @apiNote This method is designed to be used in Spring Security's {@code @PreAuthorize} annotations.
     *          For example: {@code @PreAuthorize("@userSecurity.isUserAllowedToModify(#userId)")}
     * @implSpec The method retrieves the current {@link Authentication} from the {@link SecurityContextHolder}.
     *           It first checks for administrative roles. If the user is not an admin, it then attempts to
     *           cast the principal to a {@link User} object to compare IDs. If the principal is not a {@link User}
     *           object (e.g., a generic {@link org.springframework.security.core.userdetails.UserDetails}),
     *           it returns {@code false} as the numeric ID cannot be reliably extracted.
     * @see org.springframework.security.access.prepost.PreAuthorize
     * @see me.amlu.authserver.user.model.User
     */
    public boolean isUserAllowedToModify(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Admins can modify any user.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_SUPER_ADMIN") ||
                        a.getAuthority().equals("ROLE_ROOT"));
        if (isAdmin) {
            return true;
        }

        // Regular users can only modify themselves.
        Object principal = authentication.getPrincipal();
        if (principal instanceof User currentUser) {
            // The principal is our full User object, we can get the ID directly.
            // This is the expected and most secure path.
            return currentUser.getId().equals(userId);
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // This path indicates that the principal is a generic UserDetails object, not our custom User.
            // This can happen if the AuthenticationProvider doesn't set the custom User object as the principal.
            // We cannot reliably get the numeric ID from a generic UserDetails, as it only provides a username.
            // For now, we return false as we cannot confirm ownership.
            return false;
        }

        return false;
    }
}
