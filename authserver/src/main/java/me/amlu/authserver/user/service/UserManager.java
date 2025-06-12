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

import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityNotFoundException;
import me.amlu.authserver.oauth2.repository.AuthorityRepository;
import me.amlu.authserver.oauth2.repository.OAuth2AuthorizationConsentRepository;
import me.amlu.authserver.oauth2.repository.OAuth2AuthorizationRepository;
import me.amlu.authserver.passkey.repository.PasskeyCredentialRepository;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.model.vo.HashedPassword;
import me.amlu.authserver.user.model.vo.PhoneNumber;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
@Transactional
public class UserManager implements UserServiceInterface {
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final OAuth2AuthorizationRepository oauth2AuthorizationRepository;
    private final OAuth2AuthorizationConsentRepository oauth2AuthorizationConsentRepository;
    private final PersistentTokenRepository persistentTokenRepository;


    public UserManager(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthorityRepository authorityRepository, PasskeyCredentialRepository passkeyCredentialRepository, OAuth2AuthorizationRepository oauth2AuthorizationRepository, OAuth2AuthorizationConsentRepository oauth2AuthorizationConsentRepository, PersistentTokenRepository persistentTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.oauth2AuthorizationRepository = oauth2AuthorizationRepository;
        this.oauth2AuthorizationConsentRepository = oauth2AuthorizationConsentRepository;
        this.persistentTokenRepository = persistentTokenRepository;
    }

    @Override
    @Timed(value = "authserver.usermanager.failedlogin", description = "Time taken to handle failed login attempt")
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
    @Timed(value = "authserver.usermanager.successfullogin", description = "Time taken to handle successful login")
    public void handleSuccessfulLogin(String usernameEmail) {
        log.debug("Handling successful login for username/email: {}", usernameEmail);
        userRepository.findByEmail_Value(usernameEmail).ifPresent(user -> {
            if (user.getAccountStatus().getFailedLoginAttempts() > 0 ||
                    !user.getAccountStatus().isAccountNonLocked()) {
                log.info("Resetting login failures for user {} (ID: {}).", usernameEmail, user.getId());
                user.recordSuccessfulLogin(); // This also reset login failures count
                userRepository.save(user);
                log.debug("Saved user {} (ID: {}) after successful login and failure reset.", usernameEmail, user.getId());
            } else {
                log.debug("No login failures to reset for user {} (ID: {}).", usernameEmail, user.getId());
            }
        });
    }

    @Transactional
    @Override
    @Timed(value = "authserver.usermanager.create", description = "Time taken to create user")
    public User createUser(String givenName, String middleName, String surname, String nickname,
                           String email, String rawPassword, String mobileNumber,
                           String defaultRegion, String recoveryEmailRaw) { // ADDED middleName and changed to recoveryEmailRaw
        log.info("Attempting to create user: email={}, recoveryEmail={}, givenName={}", email, recoveryEmailRaw, givenName);

        EmailAddress emailVO = new EmailAddress(email);
        EmailAddress recoveryEmailVO = StringUtils.hasText(recoveryEmailRaw) ? new EmailAddress(recoveryEmailRaw) : null; // Process recovery email

        // Check for primary email and recovery email equality
        if (recoveryEmailVO != null && recoveryEmailVO.equals(emailVO)) {
            log.warn("Recovery email {} cannot be the same as the primary email {} for new user.", recoveryEmailRaw, email);
            throw new IllegalArgumentException("Recovery email cannot be the same as the primary email.");
        }

        // Check for primary email conflict
        if (userRepository.existsByEmail_Value(email)) {
            log.warn("Primary email {} already exists.", email);
            throw new DataIntegrityViolationException("User with this primary email already exists.");
        }
        // Check for recovery email conflict if provided and if it should be unique
        if (recoveryEmailVO != null && userRepository.existsByRecoveryEmail_Value(recoveryEmailVO.getValue())) {
            log.warn("Recovery email {} already exists.", recoveryEmailVO.getValue());
            throw new DataIntegrityViolationException("User with this recovery email already exists.");
        }


        HashedPassword hashedPasswordVO = null;
        if (StringUtils.hasText(rawPassword)) { // Password can be optional for Passkey-first
            hashedPasswordVO = new HashedPassword(passwordEncoder.encode(rawPassword));
            log.debug("Password provided and will be hashed for new user: {}", email);
        } else {
            log.debug("No password provided for new user (passkey-first scenario likely): {}", email);
        }
        PhoneNumber phoneNumberVO = PhoneNumber.ofNullable(mobileNumber, defaultRegion);
        log.debug("Processed phone number for new user {}: {}", email, (phoneNumberVO != null ? phoneNumberVO.e164Value() : "N/A"));

        User newUser = User.builder()
                .givenName(givenName)
                .middleName(middleName) // Include optional middle name
                .surname(surname)
                .nickname(nickname)
                .email(emailVO)
                .recoveryEmail(recoveryEmailVO) // Using the new recoveryEmail method
                .password(hashedPasswordVO) // Can be null
                .externalId(User.generateWebAuthnUserHandle()) // Generate external ID for WebAuthn credentials in a byte array. Must be generated as it needs to be populated.
                .mobileNumber(phoneNumberVO)
                .build();
        log.debug("Built new User entity for email: {}", email);

        authorityRepository.findByAuthority("ROLE_USER").ifPresentOrElse(
                newUser::assignAuthority,
                () -> log.warn("Default authority 'ROLE_USER' not found. New user {} will have no roles.", email)
        );
        log.debug("Assigned default authorities (if found) to new user: {}", email);

        User savedUser = userRepository.save(newUser);
        log.info("Successfully created and saved user: email={}, recoveryEmail={}, userId={}",
                savedUser.getEmail().getValue(),
                (savedUser.getRecoveryEmail() != null ? savedUser.getRecoveryEmail().getValue() : "N/A"),
                savedUser.getId());
        return savedUser;
    }

    @Transactional
    @Override
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @Timed(value = "authserver.usermanager.updateprofile", description = "Time taken to update user profile")
    public User updateUserProfile(Long userId, String newGivenName, String newMiddleName, String newSurname,
                                  String newNickname, String newMobileNumber, String defaultRegion,
                                  String newRecoveryEmail) { // ADDED newMiddleName and changed to newRecoveryEmail
        log.info("Attempting to update profile for userId: {}. Updates: givenName={}, middleName={}, surname={}, nickname={}, mobileNumber={}, recoveryEmail={}",
                userId,
                (StringUtils.hasText(newGivenName) ? newGivenName : "[NO_CHANGE]"),
                (newMiddleName != null ? (newMiddleName.isEmpty() ? "[CLEAR]" : newMiddleName) : "[NO_CHANGE]"),
                (newSurname != null ? (newSurname.isEmpty() ? "[CLEAR]" : newSurname) : "[NO_CHANGE]"),
                (newNickname != null ? (newNickname.isEmpty() ? "[CLEAR]" : newNickname) : "[NO_CHANGE]"),
                (newMobileNumber != null ? newMobileNumber : "[NO_CHANGE]"),
                (newRecoveryEmail != null ? (newRecoveryEmail.isEmpty() ? "[CLEAR]" : newRecoveryEmail) : "[NO_CHANGE]")
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {} during profile update attempt.", userId);
                    return new EntityNotFoundException("User not found with id: " + userId);
                });
        log.debug("Found user for profile update: userId={}", userId);

        boolean updated = false;
        if (StringUtils.hasText(newGivenName) && !newGivenName.equals(user.getGivenName())) {
            user.updateGivenName(newGivenName);
            log.debug("Updated givenName for userId: {}", userId);
            updated = true;
        }
        // Allow clearing middleName by passing null or empty string
        if (newMiddleName != null) {
            String currentMiddleName = user.getMiddleName();
            String targetMiddleName = newMiddleName.isEmpty() ? null : newMiddleName;
            if (!Objects.equals(currentMiddleName, targetMiddleName)) {
                user.updateMiddleName(targetMiddleName);
                log.debug("Updated middleName for userId: {}", userId);
                updated = true;
            }
        }
        // Allow clearing surname and nickname by passing null or empty string
        if (newSurname != null) { // Check for null to differentiate from not wanting to update
            String currentSurname = user.getSurname();
            String targetSurname = newSurname.isEmpty() ? null : newSurname;
            if (!Objects.equals(currentSurname, targetSurname)) {
                user.updateSurname(targetSurname);
                log.debug("Updated surname for userId: {}", userId);
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

        // Update recovery email
        if (newRecoveryEmail != null) { // If an update attempt for recoveryEmail is made
            EmailAddress newRecoveryEmailVO = StringUtils.hasText(newRecoveryEmail) ? new EmailAddress(newRecoveryEmail) : null;
            if (!Objects.equals(user.getRecoveryEmail(), newRecoveryEmailVO)) {
                // Check for conflict if new recovery email is not null and different from primary email
                if (newRecoveryEmailVO != null && newRecoveryEmailVO.equals(user.getEmail())) {
                    log.warn("Attempt to set recovery email same as primary email for userId: {}", userId);
                    throw new IllegalArgumentException("Recovery email cannot be the same as the primary email.");
                }
                // Check for conflict with other users' recovery emails if recoveryEmail is unique
                if (newRecoveryEmailVO != null && userRepository.existsByRecoveryEmail_ValueAndIdNot(newRecoveryEmailVO.getValue(), userId)) {
                    log.warn("Recovery email {} already in use by another user.", newRecoveryEmailVO.getValue());
                    throw new DataIntegrityViolationException("This recovery email is already in use.");
                }
                user.updateRecoveryEmail(newRecoveryEmailVO); // Using the new method name
                log.debug("Updated recoveryEmail for userId: {}", userId);
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
    @Timed(value = "authserver.usermanager.changepassword", description = "Time taken to change user password")
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

    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @Transactional
    @Override
    @Timed(value = "authserver.usermanager.deleteaccount", description = "Time taken to delete user account")
    public void deleteUserAccount(Long userId) {
        log.info("Attempting to delete account for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {} during account deletion attempt.", userId);
                    return new EntityNotFoundException("User not found with id: " + userId);
                });
        log.debug("Found user for account deletion: userId={}, principalName={}", userId, user.getUsername());

        String principalName = user.getUsername(); // This is the email, used as principal in OAuth2

        // 1. Deleting associated PasskeyCredentials
        log.debug("Deleting passkey credentials for userId: {}", userId);
        passkeyCredentialRepository.deleteByUserId(userId);

        // 2. Deleting OAuth2Authorizations (invalidates access and refresh tokens)
        log.debug("Deleting OAuth2 authorizations for principalName: {}", principalName);
        oauth2AuthorizationRepository.deleteAllByPrincipalName(principalName);

        // 3. Deleting OAuth2AuthorizationConsents
        log.debug("Deleting OAuth2 authorization consents for principalName: {}", principalName);
        oauth2AuthorizationConsentRepository.deleteByIdPrincipalName(principalName);

        // 4. Finally, deleting the User entity
        // This will also cascade delete related entities if configured in User (e.g., user_authorities join table)

        // RememberMe - Remove/invalidate user token
        persistentTokenRepository.removeUserTokens(principalName);
        log.debug("Removed persistent tokens for user: {}", principalName);

        user.setDeletedAt(Instant.now()); // Or set a boolean flag
        user.disableAccount();
        log.debug("Deleting user entity for userId: {}", userId);
        userRepository.delete(user);

        // 5. Active HTTP Session Invalidation:
        // For standard HTTP sessions managed by Spring Security,
        // the next request from this user will fail authentication because the UserDetails
        // service (JpaUserDetailsService) will no longer find the user.
        // This typically leads to session invalidation by Spring Security.
        // Proactive invalidation of all distributed sessions is more complex and
        // often not required unless specific high-security needs demand it.

        log.info("Successfully deleted account and associated data for userId: {}, principalName: {}", userId, principalName);
    }
}
