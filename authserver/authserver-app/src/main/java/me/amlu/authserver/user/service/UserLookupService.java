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

import me.amlu.authserver.user.model.User;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * Service for looking up users from various sources.
 */
public interface UserLookupService {
    /**
     * Resolves the application-specific User entity from the given
     * Spring Security Authentication object.
     *
     * @param authentication The Spring Security Authentication object.
     * @return The resolved User entity, or null if the user cannot be resolved.
     */
    User getAppUserFromAuthentication(Authentication authentication);

    /**
     * Finds a user by their ID.
     *
     * @param userId The user ID
     * @return Optional containing the user if found
     */
    Optional<User> getUserById(Long userId);

    /**
     * Finds a user by their email.
     *
     * @param email The user's email
     * @return Optional containing the user if found
     */
    Optional<User> getUserByEmail(String email);
}
