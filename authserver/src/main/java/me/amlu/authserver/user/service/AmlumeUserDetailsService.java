/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.service;

import io.micrometer.core.annotation.Timed;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AmlumeUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmlumeUserDetailsService.class);

    private final UserRepository userRepository;

    public AmlumeUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true) // Good practice for read-only database operations
    @Timed(value = "authserver.userdetailsservice.load", description = "Time taken to load user by username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.info("Loading user by username (email): {}", username);
        User user = userRepository.findByEmail_Value(username)
                .orElseThrow(() -> {
                    LOGGER.warn("User details not found for the user (email): {}", username);
                    return new UsernameNotFoundException("User details not found for the user: " + username);
                });

        // The User entity itself implements UserDetails.
        // Its getAuthorities() method should return Collection<? extends GrantedAuthority>.
        // Its getPassword() method should return the hashed password.
        // Its getUsername() method should return the email.
        // And the account status methods (isAccountNonExpired, etc.) should be correctly implemented.
        LOGGER.info("User found: {}, Enabled: {}, AccountNonLocked: {}", user.getUsername(), user.isEnabled(), user.isAccountNonLocked());

        // Build Spring Security UserDetails object with roles/authorities
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // Hashed password
                .roles(user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray(String[]::new))
                .build();
//        return user; // Return the User entity directly
    }
}
