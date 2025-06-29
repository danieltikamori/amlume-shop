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
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Provides the current user's ID for JPA Auditing (@CreatedBy, @LastModifiedBy).
 * It inspects the SecurityContext to find the authenticated principal.
 */
public class SecurityAuditorAware implements AuditorAware<Long> {
    @Override
    @NonNull
    public Optional<Long> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .flatMap(principal -> {
                    if (principal instanceof User user) {
                        return Optional.ofNullable(user.getId());
                    }
                    // Fallback for other UserDetails implementations, though less common in this setup.
                    if (principal instanceof UserDetails) {
                        // This path is a fallback. In this application, the principal should always be
                        // the full 'User' entity. If this path is taken, it might indicate an
                        // unexpected change in the authentication flow.
                        // A lookup by username would be needed here, which requires injecting the UserRepository.
                        // For simplicity and since our flow returns the full User, we'll return empty.
                        return Optional.empty();
                    }
                    // Handle anonymousUser or other principal types
                    if ("anonymousUser".equals(principal)) {
                        return Optional.empty();
                    }
                    // Could return a system user ID for anonymous/system actions if needed.
                    // For example: return Optional.of(SYSTEM_USER_ID);
                    return Optional.empty();
                });
    }
}
