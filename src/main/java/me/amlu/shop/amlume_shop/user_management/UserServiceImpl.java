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
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.RoleNotFoundException;
import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
import me.amlu.shop.amlume_shop.exceptions.UserAlreadyExistsException;
import me.amlu.shop.amlume_shop.model.AppRole;
import me.amlu.shop.amlume_shop.payload.UserDTO;
import me.amlu.shop.amlume_shop.payload.user.UserRegistrationRequest;
import me.amlu.shop.amlume_shop.service.CacheService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static me.amlu.shop.amlume_shop.commons.Constants.*;

//The UserService should primarily be responsible for managing user data (creating, updating, retrieving users)

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceImpl userService;
    private final CacheService cacheService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, UserServiceImpl userService, CacheService cacheService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.cacheService = cacheService;
    }

    @Override
    public User registerUser(@Valid UserRegistrationRequest request) throws RoleNotFoundException {
        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        // Create new user with hashed password
        User user = User.builder()
                .authenticationInfo(new AuthenticationInfo(request.getUsername(), passwordEncoder.encode(request.getPassword())))
                .contactInfo(ContactInfo.builder().userEmail(new UserEmail(request.getUserEmail())).build())
                .mfaInfo(MfaInfo.builder().mfaEnabled(request.isMfaEnabled()).build())
                .accountStatus(AccountStatus.builder().lastLoginTime(Instant.now()).build())
                .build();

        // Assign default roles
        Set<UserRole> roles = new HashSet<>();

        Optional<UserRole> customerRoleOptional = Optional.of(new UserRole(AppRole.ROLE_CUSTOMER));
        UserRole customerRole = customerRoleOptional.orElseThrow(() -> new RoleNotFoundException("Role " + AppRole.ROLE_CUSTOMER + " not found"));
        roles.add(customerRole);

        user.setRoles(roles); // Set the roles

//        // Email verification logic
//        String verificationToken = generateVerificationToken(request.getEmail());
//        sendVerificationEmail(request.getEmail(), verificationToken); // Integration with email service
//        user.setEmailVerificationToken(verificationToken); // Store token (for later verification)
//        user.setEmailVerified(false); // Initially not verified

        return userRepository.save(user);
    }

//    public boolean authenticateUser(String username, String password) {
//        User user = userRepository.findByUsername(username)
//                .orElse(null);
//        if (user == null) {
//            return false;
//        }
//        return passwordEncoder.matches(password, user.getPassword());
//    }

    public User findUserByUsername(String username) {
        try {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        } catch (Exception e) {
            throw new UsernameNotFoundException(USER_NOT_FOUND);
        }
    }

    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Cacheable("currentUser")
    public User getCurrentUser() throws UnauthorizedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("No authenticated user found");
        }

        return userRepository.findByUsername(authentication.getName()).orElseThrow();
    }

    // Optional: Method to get just the user ID if that's all you need
    @Cacheable("currentUserId")
    public Long getCurrentUserId() {  // For Long type ID
        return userService.getCurrentUser().getUserId();
    }


    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    // TOCHECK : Update
    @Override
    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    @Override
    public UserDetails getUserDetails(String userId) {
        return cacheService.getOrCache(
                USER_CACHE,
                USER_CACHE_KEY_PREFIX + userId,
                () -> userRepository.findUserDetails(userId)
        );
    }

                // TOCHECK : Update, safety
    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // TOCHECK : Update, safety
    @Override
    public User updateUser(User user) {
        cacheService.invalidate(USER_CACHE, USER_CACHE_KEY_PREFIX + user.getUserId());
        return userRepository.save(user);
    }

    public void updateUserProfile(String userId, User user) {
        // Update in database
        userRepository.update(user);
        // Invalidate cached version
        cacheService.invalidate(USER_CACHE, USER_CACHE_KEY_PREFIX + userId);
    }

    // Use logical deletion (softDelete)
    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException(USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
        log.info("User deleted with ID: {}", userId);

    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByContactInfoEmail(email);
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        return userRepository.existsByUsername(username) || userRepository.existsByContactInfoEmail(email);
    }

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public boolean hasRole(User user, String role) {
        return user.getRoles().stream()
                .anyMatch(userRole -> userRole.getRoleName().name().equals(role));
    }

    @Transactional
    public void incrementFailedLogins(User user) {
        // Update failedLoginAttempts
        userRepository.updateFailedLoginAttempts(user.getUserId(), user.getAccountStatus().getFailedLoginAttempts() + 1);
        if (user.getAccountStatus().getFailedLoginAttempts() + 1 >= MAX_FAILED_ATTEMPTS) {
            userService.lockUser(user);
        }
    }


    @Transactional
    public void lockUser(User user) {
        userRepository.updateAccountLockStatus(user.getUserId(), false, Instant.now());
    }

    @Transactional
    public boolean unlockWhenTimeExpired(User user) {
        Instant lockTime = user.getAccountStatus().getLockTime();
        if (lockTime != null) {
            long lockTimeInMillis = lockTime.toEpochMilli();
            long currentTimeInMillis = System.currentTimeMillis();

            if (currentTimeInMillis - lockTimeInMillis >= LOCK_TIME_DURATION) {
                userRepository.unlockUser(user.getUserId());
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        userRepository.updateFailedLoginAttempts(user.getUserId(), 0);
    }

    @Transactional
    public void resetFailedLogins(User user) {
        // Update failedLoginAttempts
        userRepository.updateFailedLoginAttempts(user.getUserId(), 0);
    }

    @Transactional
    public void updateLastLoginTime(User user) {
        // Update lastLoginTime
        userRepository.updateLastLoginTime(user.getUserId(), Instant.now());
    }

    public boolean isValidUser(UserDTO user) {
        return user != null && user.getUserId() != null && user.getUsername() != null && user.getUserEmail() != null;
    }
}

