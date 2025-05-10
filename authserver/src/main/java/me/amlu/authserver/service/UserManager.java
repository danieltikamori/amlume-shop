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
import me.amlu.authserver.model.User;
import me.amlu.authserver.model.vo.EmailAddress;
import me.amlu.authserver.model.vo.HashedPassword;
import me.amlu.authserver.model.vo.PhoneNumber;
import me.amlu.authserver.repository.AuthorityRepository;
import me.amlu.authserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class UserManager implements UserServiceInterface {
    // Add SLF4J Logger instance
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;

    public UserManager(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthorityRepository authorityRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
    }

    @Override
    public void handleFailedLogin(String usernameEmail) {
        log.debug("Handling failed login attempt for username/email: {}", usernameEmail);
        User user = userRepository.findByEmail_Value(usernameEmail)
                .orElse(null);

        if (user != null) {
            log.info("User found for failed login: {}. Recording login failure.", usernameEmail);
            user.recordLoginFailure();
            if (user.isLoginAttemptsExceeded() && user.getAccountStatus().isAccountNonLocked()) {
                // Lock for, e.g., 30 minutes
                user.lockAccount(Duration.ofMinutes(30));
                log.warn("User account {} (ID: {}) locked due to excessive failed login attempts.", usernameEmail, user.getId());
            }
            userRepository.save(user);
            log.debug("Saved user {} (ID: {}) after failed login attempt.", usernameEmail, user.getId());
        } else {
            log.warn("User not found for failed login attempt: {}", usernameEmail);
        }
    }

    // Method called by authentication success handler
    @Override
    public void handleSuccessfulLogin(String usernameEmail) {
        log.debug("Handling successful login for username/email: {}", usernameEmail);
        userRepository.findByEmail_Value(usernameEmail).ifPresent(user -> {
            if (user.getAccountStatus().getFailedLoginAttempts() > 0 ||
                    !user.getAccountStatus().isAccountNonLocked()) {
                log.info("Resetting login failures for user {} (ID: {}).", usernameEmail, user.getId());
                user.resetLoginFailures();
                userRepository.save(user);
                log.debug("Saved user {} (ID: {}) after successful login and failure reset.", usernameEmail, user.getId());
            } else {
                log.debug("No login failures to reset for user {} (ID: {}).", usernameEmail, user.getId());
            }
        });
    }

    @Transactional
    @Override
    public User createUser(String firstName, String lastName, String nickname, String email, String rawPassword, String mobileNumber, String defaultRegion) {
        log.info("Attempting to create user: email={}, firstName={}", email, firstName);
        // Avoid logging rawPassword

        EmailAddress emailVO = new EmailAddress(email);
        HashedPassword hashedPasswordVO = null;
        if (StringUtils.hasText(rawPassword)) { // Password can be optional for Passkey-first
            hashedPasswordVO = new HashedPassword(passwordEncoder.encode(rawPassword));
            log.debug("Password provided and will be hashed for new user: {}", email);
        } else {
            log.debug("No password provided for new user (passkey-first scenario likely): {}", email);
        }
        PhoneNumber phoneNumberVO = PhoneNumber.ofNullable(mobileNumber, defaultRegion);
        log.debug("Processed phone number for new user {}: {}", email, (phoneNumberVO != null ? phoneNumberVO.getE164Value() : "N/A"));

        User newUser = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .nickname(nickname)
                .email(emailVO)
                .password(hashedPasswordVO) // Can be null
                .externalId(UUID.randomUUID().toString()) // Must be generated as it needs to be populated.
                .mobileNumber(phoneNumberVO)
                .build();
        log.debug("Built new User entity for email: {}", email);

        authorityRepository.findByAuthority("ROLE_USER").ifPresentOrElse(
                newUser::assignAuthority,
                () -> log.warn("Default authority 'ROLE_USER' not found. New user {} will have no roles.", email)
        );
        log.debug("Assigned default authorities (if found) to new user: {}", email);

        User savedUser = userRepository.save(newUser);
        log.info("Successfully created and saved user: email={}, userId={}", savedUser.getEmail().getValue(), savedUser.getId());
        return savedUser;
    }

    @Transactional
    @Override
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')") // Or based on who can update profile
    public User updateUserProfile(Long userId, String newFirstName, String newLastName, String newNickname, String newMobileNumber, String defaultRegion) {
        log.info("Attempting to update profile for userId: {}. Provided updates: firstName={}, lastName={}, nickname={}, mobileNumber={}",
                userId,
                (StringUtils.hasText(newFirstName) ? newFirstName : "[NO_CHANGE]"),
                (newLastName != null ? (newLastName.isEmpty() ? "[CLEAR]" : newLastName) : "[NO_CHANGE]"),
                (newNickname != null ? (newNickname.isEmpty() ? "[CLEAR]" : newNickname) : "[NO_CHANGE]"),
                (newMobileNumber != null ? newMobileNumber : "[NO_CHANGE]")
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {} during profile update attempt.", userId);
                    return new EntityNotFoundException("User not found with id: " + userId);
                });
        log.debug("Found user for profile update: userId={}", userId);

        boolean updated = false;
        if (StringUtils.hasText(newFirstName) && !newFirstName.equals(user.getFirstName())) {
            user.updateFirstName(newFirstName);
            log.debug("Updated firstName for userId: {}", userId);
            updated = true;
        }
        // Allow clearing lastName and nickname by passing null or empty string
        if (newLastName != null) { // Check for null to differentiate from not wanting to update
            String currentLastName = user.getLastName();
            String targetLastName = newLastName.isEmpty() ? null : newLastName;
            if (!Objects.equals(currentLastName, targetLastName)) {
                user.updateLastName(targetLastName);
                log.debug("Updated lastName for userId: {}", userId);
                updated = true;
            }
        }
        if (newNickname != null) {
            String currentNickname = user.getNickname();
            String targetNickname = newNickname.isEmpty() ? null : newNickname;
            if (!Objects.equals(currentNickname, targetNickname)) {
                user.updateNickname(targetNickname);
                log.debug("Updated nickname for userId: {}", userId);
                updated = true;
            }
        }

        if (newMobileNumber != null) {
            PhoneNumber newPhoneNumberVO = PhoneNumber.ofNullable(newMobileNumber, defaultRegion);
            if (!Objects.equals(user.getMobileNumber(), newPhoneNumberVO)) {
                user.updateMobileNumber(newPhoneNumberVO);
                log.debug("Updated mobileNumber for userId: {}", userId);
                updated = true;
            }
        }

        if (updated) {
            User savedUser = userRepository.save(user);
            log.info("Successfully updated profile for userId: {}", userId);
            return savedUser;
        } else {
            log.info("No profile changes detected for userId: {}. Returning existing user.", userId);
            return user;
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')") // Or based on who can change password
    public void changeUserPassword(Long userId, String newRawPassword) {
        log.info("Attempting to change password for userId: {}", userId);
        // DO NOT log newRawPassword

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {} during password change attempt.", userId);
                    return new EntityNotFoundException("User not found with id: " + userId);
                });
        log.debug("Found user for password change: userId={}", userId);

        HashedPassword newHashedPassword = new HashedPassword(passwordEncoder.encode(newRawPassword));
        user.changePassword(newHashedPassword);
        userRepository.save(user);
        log.info("Successfully changed password for userId: {}", userId);
    }

    // Method to be called by an admin to manually unlock an account
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    @Override
    public void adminUnlockUser(Long userId) {
        log.info("Admin attempting to unlock account for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {} during admin unlock attempt.", userId);
                    return new EntityNotFoundException("User not found with id: " + userId);
                });
        log.debug("Found user for admin unlock: userId={}", userId);

        user.unlockAccount();
        user.resetLoginFailures(); // Also reset failure counts
        userRepository.save(user);
        log.info("Admin successfully unlocked account and reset login failures for userId: {}", userId);
    }

    // Method to be called by an admin to manually disable/enable an account
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    @Override
    public void adminSetUserEnabled(Long userId, boolean enabled) {
        log.info("Admin attempting to set enabled status to {} for userId: {}", enabled, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {} during admin enable/disable attempt.", userId);
                    return new EntityNotFoundException("User not found with id: " + userId);
                });
        log.debug("Found user for admin enable/disable: userId={}", userId);

        if (enabled) {
            user.enableAccount();
        } else {
            user.disableAccount();
        }
        userRepository.save(user);
        log.info("Admin successfully set enabled status to {} for userId: {}", enabled, userId);
    }
}