/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.audit;

import me.amlu.shop.amlume_shop.user_management.User; // Import your User entity
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

// Option 1: Use @Component("auditorAware") - Spring finds it automatically
// @Component("auditorAware")
// Option 2: Use plain class and define @Bean in AmlumeShopApplication (Chosen below)
public class SecurityAuditorAware implements AuditorAware<Long> {

    private final Logger log = LoggerFactory.getLogger(SecurityAuditorAware.class);

    @NotNull
    @Override
    public Optional<Long> getCurrentAuditor() {
        // Get the current Authentication object from Spring Security's context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if authentication is valid, user is authenticated, and not anonymous
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            authentication.getPrincipal() == null || // Added null check for principal
            "anonymousUser".equals(authentication.getPrincipal().toString())) { // Check against anonymous user string representation
            // No authenticated user or it's the anonymous user
            return Optional.empty(); // Indicate no specific user (e.g., for system tasks)
                                     // Or return Optional.of(SYSTEM_USER_ID) if we have a dedicated system user ID
        }

        // Get the principal (the user object)
        Object principal = authentication.getPrincipal();

        // Check if the principal is an instance of the User entity
        // The User entity implements UserDetails, so this should work
        if (principal instanceof User) {
            // Cast and get the ID (which is Long)
            Long userId = ((User) principal).getUserId();
            return Optional.of(userId);
        } else {
            // Handle cases where the principal might be something else (e.g., just a username String)
            // This might happen depending on our security configuration.
            // If principal is String, we might need to look up the user by username.
            // For now, return empty if it's not the expected User entity.
            // We might want to log a warning here if this case occurs unexpectedly.
            log.warn("AuditorAware: Principal is not an instance of User: {}", principal.getClass().getName());
             System.err.println("AuditorAware: Principal is not an instance of User: " + principal.getClass().getName());
            return Optional.empty();
        }
    }
}