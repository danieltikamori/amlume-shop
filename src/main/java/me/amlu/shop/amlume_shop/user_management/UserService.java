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
import me.amlu.shop.amlume_shop.payload.user.UserProfileUpdateRequest;
import me.amlu.shop.amlume_shop.payload.user.UserRegistrationRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static me.amlu.shop.amlume_shop.commons.Constants.CURRENT_USER_CACHE;
import static me.amlu.shop.amlume_shop.commons.Constants.USERS_CACHE;

public interface UserService {

    // Consider if this should return UserDetails or a specific UserProfileDTO
    // Caching strategy depends on return type and frequency of access
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    User getCurrentUser();

    Long getCurrentUserId();

    User getUserById(Long userId);

//    User getUserByUsername(String username);

    User getUserByEmail(String email);

    User getUserByUsernameOrEmail(String usernameOrEmail);

//    User getUserProfile(Long userId);

//    UserDetails getUserDetails(String userId);

//    User createUser(User user);

//    User updateUser(User user);

    UserDetails getUserDetails(Long userId);

//    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#result.userId") // Evict relevant caches
    User updateUserProfile(Long userId, @Valid UserProfileUpdateRequest profileRequest);

    void deleteUser(Long userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameOrEmail(String username, String email);

    boolean existsById(Long userId);

    User registerUser(@Valid UserRegistrationRequest request) throws RoleNotFoundException;

//    boolean authenticateUser(String username, String password);

    User findUserByUsername(String username);

//    boolean hasRole(User user, UserRole role);

    boolean hasRole(User user, String role);

    @Transactional
        // Ensure atomicity
    void incrementFailedLogins(String username);

    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict relevant caches
    void lockUserAccount(Long userId);

    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict relevant caches
    boolean unlockAccountIfExpired(Long userId);

    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict relevant caches
    void resetFailedLoginAttempts(Long userId);

    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict relevant caches
    void updateLastLoginTime(Long userId);
}
