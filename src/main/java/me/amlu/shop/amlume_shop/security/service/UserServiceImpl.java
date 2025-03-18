/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.RoleNotFoundException;
import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
import me.amlu.shop.amlume_shop.exceptions.UserAlreadyExistsException;
import me.amlu.shop.amlume_shop.model.AppRole;
import me.amlu.shop.amlume_shop.model.Role;
import me.amlu.shop.amlume_shop.model.User;
import me.amlu.shop.amlume_shop.payload.user.UserRegistrationRequest;
import me.amlu.shop.amlume_shop.repositories.RoleRepository;
import me.amlu.shop.amlume_shop.repositories.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceImpl userService;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, UserServiceImpl userService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @Override
    public User registerUser(@Valid UserRegistrationRequest request) throws RoleNotFoundException {
        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        // Create new user with hashed password
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserEmail(request.getUserEmail());
        user.setMfaEnabled(request.isMfaEnabled());
        user.setLastLoginTime(Instant.now());

        // Assign default roles
        Set<Role> roles = new HashSet<>();

//        Optional<Role> userRoleOptional = Optional.ofNullable(roleRepository.findByRoleName(AppRole.ROLE_USER));
//        Role userRole = userRoleOptional.orElseThrow(() -> new RoleNotFoundException("Role " + AppRole.ROLE_USER + " not found"));
//        roles.add(userRole);

        Optional<Role> customerRoleOptional = Optional.ofNullable(roleRepository.findByRoleName(AppRole.ROLE_CUSTOMER));
        Role customerRole = customerRoleOptional.orElseThrow(() -> new RoleNotFoundException("Role " + AppRole.ROLE_CUSTOMER + " not found"));
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
        return null;
    }

    @Override
    public User getUserByEmail(String email) {
        return null;
    }

    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) {
        return null;
    }

    @Override
    public User getUserProfile(Long userId) {
        return null;
    }

    @Override
    public User createUser(User user) {
        return null;
    }

    @Override
    public User updateUser(User user) {
        return null;
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
        return false;
    }

    @Override
    public boolean existsByEmail(String email) {
        return false;
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        return false;
    }

    @Override
    public boolean existsById(Long userId) {
        return false;
    }

    @Override
    public boolean existsByUsernameAndIdNot(String username, Long userId) {
        return false;
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long userId) {
        return false;
    }

    @Override
    public boolean existsByUsernameOrEmailAndIdNot(String username, String email, Long userId) {
        return false;
    }

    @Override
    public boolean existsByIdNot(Long userId) {
        return false;
    }

    public void incrementFailedLogins(User user) {
//        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        int newFailedAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newFailedAttempts);

        if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockUser(user);
        }
        userRepository.save(user);
    }

    public void lockUser(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(Instant.now());
        userRepository.save(user);
    }

    public boolean unlockWhenTimeExpired(User user) {
        Instant lockTime = user.getLockTime();
        if (lockTime != null) {
            long lockTimeInMillis = lockTime.toEpochMilli();
            long currentTimeInMillis = System.currentTimeMillis();

            if (currentTimeInMillis - lockTimeInMillis >= LOCK_TIME_DURATION) {
                user.setAccountNonLocked(true);
                user.setLockTime(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);

                return true;
            }
        }
        return false;
    }

    public void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

}

