/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.oauth2.service;

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

@Service("jpaUserDetailsService") // Bean name is jpaUserDetailsService
public class JpaUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(JpaUserDetailsService.class);
    private final UserRepository userRepository;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("JpaUserDetailsService: Loading user by username (email): {}", username);
        User user = userRepository.findByEmail_Value(username) // Uses @EntityGraph
                .orElseThrow(() -> {
                    log.warn("JpaUserDetailsService: User details not found for the user (email): {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        // Unproxy the user entity itself
        User unproxiedUser = (User) Hibernate.unproxy(user);

        // --- Explicitly initialize and de-proxy the authorities collection ---
        Hibernate.initialize(unproxiedUser.getAuthorities());
        Set<Authority> deProxiedAuthorities = new HashSet<>();
        Collection<? extends GrantedAuthority> authoritiesToIterate = unproxiedUser.getAuthorities();

        if (authoritiesToIterate != null && !authoritiesToIterate.isEmpty()) {
            for (GrantedAuthority grantedAuth : authoritiesToIterate) {
                if (grantedAuth instanceof Authority auth) { // Use pattern variable
                    Authority unproxiedAuth = (Authority) Hibernate.unproxy(auth);
                    // Authority.permissions is EAGER.
                    deProxiedAuthorities.add(unproxiedAuth);
                } else {
                    log.warn("JpaUserDetailsService: Skipping authority of type {} as it's not an instance of Authority",
                            grantedAuth.getClass().getName());
                }
            }
        }
        unproxiedUser.setAuthorities(deProxiedAuthorities);

        // --- Explicitly initialize and de-proxy the passkeyCredentials collection ---
        Hibernate.initialize(unproxiedUser.getPasskeyCredentials());
        Set<PasskeyCredential> deProxiedPasskeys = new HashSet<>();
        if (unproxiedUser.getPasskeyCredentials() != null && !unproxiedUser.getPasskeyCredentials().isEmpty()) {
            for (PasskeyCredential pkc : new ArrayList<>(unproxiedUser.getPasskeyCredentials())) { // Iterate over a copy
                PasskeyCredential unproxiedPkc = (PasskeyCredential) Hibernate.unproxy(pkc);
                deProxiedPasskeys.add(unproxiedPkc);
            }
        }
        unproxiedUser.setPasskeyCredentials(deProxiedPasskeys);

        log.debug("JpaUserDetailsService: User found: {}, Authorities loaded: {}, Passkeys loaded: {}",
                unproxiedUser.getUsername(),
                unproxiedUser.getAuthorities() != null ? unproxiedUser.getAuthorities().size() : "null",
                unproxiedUser.getPasskeyCredentials() != null ? unproxiedUser.getPasskeyCredentials().size() : "null");

        return unproxiedUser;
    }
}
