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

import me.amlu.authserver.common.AuthUtils;
import me.amlu.authserver.common.StringUtils;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserLookupServiceImpl implements UserLookupService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserLookupServiceImpl.class);
    private final UserRepository userRepository;

    public UserLookupServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the application-specific {@link User} entity from the given
     * Spring Security {@link Authentication} object.
     *
     * @param authentication The Spring Security Authentication object.
     * @return The resolved {@link User} entity, or {@code null} if the user
     * cannot be resolved or if the authentication is invalid.
     */
    @Override
    @Transactional(readOnly = true)
    public User getAppUserFromAuthentication(Authentication authentication) {
        return AuthUtils.getUserFromAuthentication(authentication, email -> {
            if (StringUtils.isBlank(email)) {
                return null; // For type checking in AuthUtils
            }
            return userRepository.findByEmail_ValueAndDeletedAtIsNull(email)
                    .orElseGet(() -> {
                        log.warn("No local User found for principal with email: {}. " +
                                "This could be a provisioning delay or an issue if the user should exist.", email);
                        return null;
                    });
        }, User.class);
    }

    /**
     * Finds a user by their ID.
     *
     * @param userId The user ID
     * @return Optional containing the user if found
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Finds a user by their email.
     *
     * @param email The user's email
     * @return Optional containing the user if found
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail_ValueAndDeletedAtIsNull(email);
    }
}
