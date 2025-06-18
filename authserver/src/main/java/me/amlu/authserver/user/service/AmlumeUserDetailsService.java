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
import me.amlu.authserver.oauth2.model.Authority;
import me.amlu.authserver.passkey.model.PasskeyCredential;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList; // For iterating over a copy
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service // Default bean name will be "amlumeUserDetailsService"
public class AmlumeUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AmlumeUserDetailsService.class);

    private final UserRepository userRepository;

    public AmlumeUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "authserver.userdetailsservice.load", description = "Time taken to load user by username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user by username (email): {}", username);
        User user = userRepository.findByEmail_Value(username) // Uses @EntityGraph for authorities and passkeyCredentials
                .orElseThrow(() -> {
                    log.warn("User details not found for the user (email): {}", username);
                    return new UsernameNotFoundException("User details not found for the user: " + username);
                });

        // Unproxy the user entity itself
        User unproxiedUser = (User) Hibernate.unproxy(user);

        // --- Explicitly initialize and de-proxy the authorities collection ---
        Hibernate.initialize(unproxiedUser.getAuthorities()); // Ensures the collection proxy is initialized
        Set<Authority> deProxiedAuthorities = new HashSet<>();
        Collection<? extends GrantedAuthority> authoritiesToIterate = unproxiedUser.getAuthorities();

        if (authoritiesToIterate != null && !authoritiesToIterate.isEmpty()) {
            for (GrantedAuthority grantedAuth : authoritiesToIterate) {
                if (grantedAuth instanceof Authority auth) { // Use pattern variable
                    Authority unproxiedAuth = (Authority) Hibernate.unproxy(auth);
                    // Authority.permissions is EAGER, so they should be loaded with unproxiedAuth.
                    deProxiedAuthorities.add(unproxiedAuth);
                } else {
                    log.warn("Skipping authority of type {} as it's not an instance of me.amlu.authserver.oauth2.model.Authority",
                            grantedAuth.getClass().getName());
                }
            }
        }
        // Replace the original collection
        unproxiedUser.setAuthorities(deProxiedAuthorities); // Replace with the de-proxied set

        // --- Explicitly initialize and de-proxy the passkeyCredentials collection ---
        // EntityGraph should have loaded them. This ensures elements are not proxies.
        Hibernate.initialize(unproxiedUser.getPasskeyCredentials()); // Initialize the collection from the unproxiedUser
        Set<PasskeyCredential> deProxiedPasskeys = new HashSet<>();
        // Iterate over a copy because getPasskeyCredentials() returns an unmodifiable set
        if (unproxiedUser.getPasskeyCredentials() != null && !unproxiedUser.getPasskeyCredentials().isEmpty()) {
            for (PasskeyCredential pkc : new ArrayList<>(unproxiedUser.getPasskeyCredentials())) {
                PasskeyCredential unproxiedPkc = (PasskeyCredential) Hibernate.unproxy(pkc);
                // If PasskeyCredential had its own LAZY associations that are needed in session,
                // initialize and de-proxy them here. For now, assume PasskeyCredential is simple enough.
                deProxiedPasskeys.add(unproxiedPkc);
            }
        }
        unproxiedUser.setPasskeyCredentials(deProxiedPasskeys); // Replace with the de-proxied set

        log.info("User found: {}, Authorities loaded: {}, Passkeys loaded: {}, Enabled: {}, AccountNonLocked: {}",
                unproxiedUser.getUsername(),
                unproxiedUser.getAuthorities() != null ? unproxiedUser.getAuthorities().size() : "null",
                unproxiedUser.getPasskeyCredentials() != null ? unproxiedUser.getPasskeyCredentials().size() : "null",
                unproxiedUser.isEnabled(),
                unproxiedUser.isAccountNonLocked());

        // OLD: Creating a new Spring Security User object
        // return org.springframework.security.core.userdetails.User.builder()
        //         .username(user.getUsername())
        //         .password(user.getPassword()) // Hashed password
        //         .roles(user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray(String[]::new)) // This would use the modified User.getAuthorities()
        //         .build();

        // NEW: Return the custom User entity directly, as it implements UserDetails
        // and its getAuthorities() method now returns the custom Authority objects.

        // Create a fully detached User instance using the builder
        User detachedUser = unproxiedUser.toBuilder().build();
        // The toBuilder() method should copy the already de-proxied collections.
        // The User constructor called by build() will then create new HashSets from these.

        // Log using the detachedUser instance
        // Example for AmlumeUserDetailsService:
        log.info("Returning detached User: {}, Authorities: {}, Passkeys: {}, Enabled: {}, AccountNonLocked: {}",
                detachedUser.getUsername(),
                detachedUser.getAuthorities() != null ? detachedUser.getAuthorities().size() : "null",
                detachedUser.getPasskeyCredentials() != null ? detachedUser.getPasskeyCredentials().size() : "null",
                detachedUser.isEnabled(),
                detachedUser.isAccountNonLocked());


        // log.debug("AmlumeUserDetailsService: Returning detached User: {}, Authorities: {}, Passkeys: {}",
        //        detachedUser.getUsername(),
        //        detachedUser.getAuthorities() != null ? detachedUser.getAuthorities().size() : "null",
        //        detachedUser.getPasskeyCredentials() != null ? detachedUser.getPasskeyCredentials().size() : "null");


        return detachedUser; // Return the fully detached User object
    }
}
