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

import java.util.ArrayList; // Used for iterating over a copy of passkeyCredentials
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to load user-specific data by username (email) for Spring Security.
 * This implementation retrieves user details from a JPA repository and ensures
 * that the returned {@link UserDetails} object, along with its collections
 * (like authorities and passkeys), are fully detached from the Hibernate session.
 * This is crucial to prevent {@link org.hibernate.LazyInitializationException}
 * when Spring Security accesses these details outside the original transaction,
 * especially if the UserDetails are serialized (e.g., to an HTTP session).
 *
 * <p><b>Detachment Process:</b></p>
 * <ol>
 *   <li>Fetch the {@link User} entity using an email (username). An {@code @EntityGraph}
 *       is expected on the repository method to eagerly fetch associated collections.</li>
 *   <li>Unproxy the root {@link User} entity using {@link Hibernate#unproxy(Object)}.</li>
 *   <li>For each relevant collection (e.g., authorities, passkey credentials):
 *       <ul>
 *           <li>Ensure the collection is initialized using {@link Hibernate#initialize(Object)} (as a safeguard,
 *               even if an entity graph is used).</li>
 *           <li>Create a new {@link Set} to hold the de-proxied elements.</li>
 *           <li>Iterate over the original collection, unproxying each element using {@link Hibernate#unproxy(Object)}.</li>
 *           <li>Add the unproxied elements to the new set.</li>
 *           <li>Replace the original collection on the (unproxied) user entity with this new set of unproxied elements.</li>
 *       </ul>
 *   </li>
 *   <li>Utilize the {@code User.toBuilder().build()} pattern. This is assumed to create a
 *       <i>new</i> {@link User} instance, copying all fields and collections, ensuring the final
 *       returned {@link UserDetails} object is completely detached and safe for use
 *       by Spring Security.</li>
 * </ol>
 */
@Service("jpaUserDetailsService") // Bean name is jpaUserDetailsService
public class JpaUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(JpaUserDetailsService.class);
    private final UserRepository userRepository;

    /**
     * Constructs the service with the required {@link UserRepository}.
     *
     * @param userRepository The repository to fetch user data.
     */
    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user by their username (which is the email in this context).
     *
     * @param username The email address of the user to load.
     * @return A fully populated and detached {@link UserDetails} object.
     * @throws UsernameNotFoundException if the user could not be found.
     */
    @Override
    @Transactional(readOnly = true) // Ensures the Hibernate session is available, optimized for read operations.
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Attempting to load user by username (email): {}", username);

        // Fetch the user. The repository method findByEmail_Value is expected to use an @EntityGraph
        // to eagerly load associated collections like authorities and passkeyCredentials.
        User user = userRepository.findByEmail_Value(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username (email): {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        // Unproxy the main User entity to ensure it's not a Hibernate proxy.
        User unproxiedUser = (User) Hibernate.unproxy(user);

        // --- Explicitly initialize and de-proxy the authorities collection ---
        // Even if an EntityGraph is used, explicit initialization and unproxying provide robustness.
        Hibernate.initialize(unproxiedUser.getAuthorities()); // Ensure collection is loaded
        Set<Authority> deProxiedAuthorities = new HashSet<>();
        Collection<? extends GrantedAuthority> authoritiesToIterate = unproxiedUser.getAuthorities();

        if (authoritiesToIterate != null && !authoritiesToIterate.isEmpty()) {
            for (GrantedAuthority grantedAuth : authoritiesToIterate) {
                if (grantedAuth instanceof Authority auth) { // Use pattern variable
                    // Unproxy individual Authority objects within the collection.
                    // Assumes Authority.permissions is EAGER or handled similarly if accessed later.
                    Authority unproxiedAuth = (Authority) Hibernate.unproxy(auth);
                    // Authority.permissions is EAGER.
                    deProxiedAuthorities.add(unproxiedAuth);
                } else {
                    log.warn("Skipping authority of type {} as it's not an instance of me.amlu.authserver.oauth2.model.Authority",
                            grantedAuth.getClass().getName());
                }
            }
        }
        // Replace the potentially proxy-backed collection with a new Set of unproxied Authority objects.
        unproxiedUser.setAuthorities(deProxiedAuthorities);

        // --- Explicitly initialize and de-proxy the passkeyCredentials collection ---
        Hibernate.initialize(unproxiedUser.getPasskeyCredentials()); // Ensure collection is loaded
        Set<PasskeyCredential> deProxiedPasskeys = new HashSet<>();
        if (unproxiedUser.getPasskeyCredentials() != null && !unproxiedUser.getPasskeyCredentials().isEmpty()) {
            // Iterate over a copy (new ArrayList<>(...)) to avoid potential issues if the underlying collection type
            // had modification restrictions during iteration, though less critical here.
            for (PasskeyCredential pkc : new ArrayList<>(unproxiedUser.getPasskeyCredentials())) {
                PasskeyCredential unproxiedPkc = (PasskeyCredential) Hibernate.unproxy(pkc);
                deProxiedPasskeys.add(unproxiedPkc);
            }
        }
        // Replace with a new Set of unproxied PasskeyCredential objects.
        unproxiedUser.setPasskeyCredentials(deProxiedPasskeys);

        log.debug("User entity processed: Username='{}', Authorities loaded: {}, Passkeys loaded: {}",
                unproxiedUser.getUsername(),
                unproxiedUser.getAuthorities() != null ? unproxiedUser.getAuthorities().size() : "0",
                unproxiedUser.getPasskeyCredentials() != null ? unproxiedUser.getPasskeyCredentials().size() : "0");

        // Create a fully detached User instance using the builder pattern.
        // This step is crucial. The User.toBuilder().build() methods should be implemented
        // to perform a deep copy of all relevant fields and collections, ensuring the
        // resulting UserDetails object is completely independent of the Hibernate session.
        User detachedUser = unproxiedUser.toBuilder().build();
        // The toBuilder() method should copy the already de-proxied collections.
        // The User constructor called by build() will then create new HashSets from these.

        log.info("Returning detached UserDetails for username: {}, Authorities: {}, Passkeys: {}, Enabled: {}, AccountNonLocked: {}",
                detachedUser.getUsername(),
                detachedUser.getAuthorities() != null ? detachedUser.getAuthorities().size() : "0",
                detachedUser.getPasskeyCredentials() != null ? detachedUser.getPasskeyCredentials().size() : "0",
                detachedUser.isEnabled(),
                detachedUser.isAccountNonLocked());

        return detachedUser; // Return the fully detached User object
    }
}
