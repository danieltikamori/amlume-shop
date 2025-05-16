/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.exceptions.RoleNotFoundException;
import me.amlu.shop.amlume_shop.exceptions.UserAlreadyExistsException;
import me.amlu.shop.amlume_shop.user_management.dto.UserProfileUpdateRequest;
import me.amlu.shop.amlume_shop.user_management.dto.UserRegistrationRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {

    /**
     * Registers a new user with administrative privileges.
     *
     * @param request The user registration request DTO.
     * @return The newly created and saved User entity with admin roles.
     * @throws UserAlreadyExistsException If the username or email is already taken.
     * @throws RoleNotFoundException      (Potentially, if role logic changes)
     */
    User registerAdminUser(@Valid UserRegistrationRequest request) throws UserAlreadyExistsException, RoleNotFoundException;

    // Consider if this should return UserDetails or a specific UserProfileDTO
    // Caching strategy depends on return type and frequency of access
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    User getCurrentUser();

    Long getCurrentUserId();

    User getUserById(Long userId);

    UserDetails getUserDetails(Long userId);

    User updateUserProfile(Long userId, @Valid UserProfileUpdateRequest profileRequest);

    void deleteUser(Long userId);


    boolean existsByEmail(String email);

    boolean existsById(Long userId);

//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    User registerUser(@Valid UserRegistrationRequest request) throws RoleNotFoundException;

//    boolean authenticateUser(String username, String password);

    User findUserByEmail(String username);

//    boolean hasRole(User user, UserRole role);

    boolean hasRole(User user, String role);

//    @Transactional
//    @Deprecated(since = "2025-05-16", forRemoval = true)
//    void incrementFailedLogins(String username);
//
//    @Transactional
//    @Deprecated(since = "2025-05-16", forRemoval = true)
//    void lockUserAccount(Long userId);
//
//    @Transactional
//    @Deprecated(since = "2025-05-16", forRemoval = true)
//    boolean unlockAccountIfExpired(Long userId);
//
//    @Transactional
//    @Deprecated(since = "2025-05-16", forRemoval = true)
//    void resetFailedLoginAttempts(Long userId);

    @Transactional
    void updateLastLoginTime(Long userId);
}
