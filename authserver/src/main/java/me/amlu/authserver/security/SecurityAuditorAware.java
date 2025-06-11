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
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

// AuditorAware bean
@Component("auditorAware")
public class SecurityAuditorAware implements AuditorAware<Long> {
    @Override
    @NonNull
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty(); // Or a system user ID
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return Optional.ofNullable(user.getId());
        } else if (principal instanceof UserDetails userDetails) {
            // If principal is UserDetails but not the User entity, you might need to
            // load the User entity based on UserDetails.getUsername() to get the ID.
            // This part depends on the UserDetailsService and principal object.
            // For simplicity, this code assumes the principal is the User entity.
            // String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            // Optional<User> user = userRepository.findByEmail_Value(username);
            // return user.map(User::getId);
            return Optional.empty(); // Placeholder if UserDetails is not the User
        }
        return Optional.empty();
    }
}
