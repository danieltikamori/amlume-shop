/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import me.amlu.authserver.model.User;
import me.amlu.authserver.model.vo.EmailAddress;
import me.amlu.authserver.model.vo.HashedPassword;
import me.amlu.authserver.model.vo.PhoneNumber;
import me.amlu.authserver.repository.AuthorityRepository;
import me.amlu.authserver.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class UserManager implements UserServiceInterface {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;

    @Override
    public void handleFailedLogin(String usernameEmail) { // Parameter name clarified
        User user = userRepository.findByEmail_Value(usernameEmail)
                .orElse(null);

        if (user != null) {
            user.recordLoginFailure();
            if (user.isLoginAttemptsExceeded() && user.getAccountStatus().isAccountNonLocked()) {
                // Lock for, e.g., 30 minutes
                user.lockAccount(Duration.ofMinutes(30));
                // Log account lockout event
                // logger.warn("User account {} locked due to excessive failed login attempts.", username);
            }
            userRepository.save(user);
        }
    }

    // Method called by authentication success handler
    @Override
    public void handleSuccessfulLogin(String usernameEmail) { // Parameter name clarified
        userRepository.findByEmail_Value(usernameEmail).ifPresent(user -> {
            if (user.getAccountStatus().getFailedLoginAttempts() > 0 ||
                    !user.getAccountStatus().isAccountNonLocked()) { // If previously failed or locked
                user.resetLoginFailures(); // This also unlocks if it was a temporary lock
                userRepository.save(user);
                // logger.info("Reset failed login attempts for user {}.", username);
            }
        });
    }

    @Transactional
    @Override
    public User createUser(String firstName, String lastName, String nickname, String email, String rawPassword, String mobileNumber, String defaultRegion) {
        EmailAddress emailVO = new EmailAddress(email);
        HashedPassword hashedPasswordVO = null;
        if (StringUtils.hasText(rawPassword)) { // Password can be optional for Passkey-first
            hashedPasswordVO = new HashedPassword(passwordEncoder.encode(rawPassword));
        }
        PhoneNumber phoneNumberVO = PhoneNumber.ofNullable(mobileNumber, defaultRegion);

        User newUser = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .nickname(nickname)
                .email(emailVO)
                .password(hashedPasswordVO) // Can be null
                .mobileNumber(phoneNumberVO)
                .build();

        authorityRepository.findByAuthority("ROLE_USER").ifPresent(newUser::assignAuthority);
        return userRepository.save(newUser);
    }

    @Transactional
    @Override
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')") // Or based on who can update profile
    public User updateUserProfile(Long userId, String newFirstName, String newLastName, String newNickname, String newMobileNumber, String defaultRegion) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (StringUtils.hasText(newFirstName)) {
            user.updateFirstName(newFirstName);
        }
        // Allow clearing lastName and nickname by passing null or empty string
        if (newLastName != null) { // Check for null to differentiate from not wanting to update
            user.updateLastName(newLastName.isEmpty() ? null : newLastName);
        }
        if (newNickname != null) { // Check for null
            user.updateNickname(newNickname.isEmpty() ? null : newNickname);
        }

        if (newMobileNumber != null) {
            user.updateMobileNumber(PhoneNumber.ofNullable(newMobileNumber, defaultRegion));
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')") // Or based on who can change password
    public void changeUserPassword(Long userId, String newRawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        HashedPassword newHashedPassword = new HashedPassword(passwordEncoder.encode(newRawPassword));
        user.changePassword(newHashedPassword);
        userRepository.save(user);
    }

    // Method to be called by an admin to manually unlock an account
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    @Override
    public void adminUnlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        user.unlockAccount();
        user.resetLoginFailures();
        userRepository.save(user);
    }

    // Method to be called by an admin to manually disable/enable an account
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    @Override
    public void adminSetUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        if (enabled) {
            user.enableAccount();
        } else {
            user.disableAccount();
        }
        userRepository.save(user);
    }
}