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
import jakarta.persistence.OptimisticLockException;
import me.amlu.authserver.exceptions.PasswordMismatchException;
import me.amlu.authserver.oauth2.model.Authority;
import me.amlu.authserver.oauth2.repository.AuthorityRepository;
import me.amlu.authserver.oauth2.repository.OAuth2AuthorizationConsentRepository;
import me.amlu.authserver.oauth2.repository.OAuth2AuthorizationRepository;
import me.amlu.authserver.passkey.repository.PasskeyCredentialRepository;
import me.amlu.authserver.security.config.PasswordPolicyConfig;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.model.vo.HashedPassword;
import me.amlu.authserver.user.model.vo.PhoneNumber;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for managing user accounts and related operations.
 * Handles user creation, authentication, profile updates, and account management.
 */
@Service
@Transactional
public class UserManager implements UserServiceInterface {
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);

    /**
     * Repository for user entity operations
     */
    private final UserRepository userRepository;
    /** Service for password encoding and verification */
    private final PasswordEncoder passwordEncoder;
    /**
     * Configuration for password policy rules
     */
    private final PasswordPolicyConfig passwordPolicyConfig;
    /**
     * Service to check if passwords have been compromised in data breaches
     */
    private final CompromisedPasswordChecker compromisedPasswordChecker;
    /** Repository for user authority/role operations */
    private final AuthorityRepository authorityRepository;
    /** Repository for passkey credential operations */
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    /** Repository for OAuth2 authorization operations */
    private final OAuth2AuthorizationRepository oauth2AuthorizationRepository;
    /** Repository for OAuth2 consent operations */
    private final OAuth2AuthorizationConsentRepository oauth2AuthorizationConsentRepository;
    /** Repository for persistent remember-me tokens */
    private final PersistentTokenRepository persistentTokenRepository;
    /**
     * Repository for user session management
     */
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;


    /**
     * Constructs a UserManager with all required dependencies.
     *
     * @param userRepository                       Repository for user entity operations
     * @param passwordEncoder                      Service for password encoding and verification
     * @param passwordPolicyConfig                 Configuration for password policy rules
     * @param compromisedPasswordChecker           Service to check if passwords have been compromised
     * @param authorityRepository                  Repository for user authority/role operations
     * @param passkeyCredentialRepository          Repository for passkey credential operations
     * @param oauth2AuthorizationRepository        Repository for OAuth2 authorization operations
     * @param oauth2AuthorizationConsentRepository Repository for OAuth2 consent operations
     * @param persistentTokenRepository            Repository for persistent remember-me tokens
     * @param sessionRepository                    Repository for user session management
     */
    public UserManager(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordPolicyConfig passwordPolicyConfig, CompromisedPasswordChecker compromisedPasswordChecker, AuthorityRepository authorityRepository, PasskeyCredentialRepository passkeyCredentialRepository, OAuth2AuthorizationRepository oauth2AuthorizationRepository, OAuth2AuthorizationConsentRepository oauth2AuthorizationConsentRepository, PersistentTokenRepository persistentTokenRepository, FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyConfig = passwordPolicyConfig;
        this.compromisedPasswordChecker = compromisedPasswordChecker;
        this.authorityRepository = authorityRepository;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.oauth2AuthorizationRepository = oauth2AuthorizationRepository;
        this.oauth2AuthorizationConsentRepository = oauth2AuthorizationConsentRepository;
        this.persistentTokenRepository = persistentTokenRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Handles failed login attempts by recording the failure and potentially locking the account.
     * 
     * @param usernameEmail The username or email used in the failed login attempt
     */
    @Override
    @Timed(value = "authserver.usermanager.failedlogin", description = "Time taken to handle failed login attempt")
    public void handleFailedLogin(String usernameEmail) {
        log.debug("Handling failed login attempt for username/email: {}", usernameEmail);
        try {
            User user = userRepository.findByEmail_ValueAndDeletedAtIsNull(usernameEmail)
                .orElse(null);

        if (user != null) {
            log.info("User found for failed login: {}. Recording login failure.", usernameEmail);
            user.recordLoginFailure();
            if (user.isLoginAttemptsExceeded() && user.getAccountStatus().isAccountNonLocked()) {
                // Lock for, e.g., 30 minutes
                user.lockAccount(Duration.ofMinutes(30));
                log.warn("User account {} (ID: {}) locked due to excessive failed login attempts.", usernameEmail, user.getId());
            }
            userRepository.save(user); // Hibernate checks version here
            log.debug("Saved user {} (ID: {}) after failed login attempt.", usernameEmail, user.getId());
        } else {
            log.warn("User not found for failed login attempt: {}", usernameEmail);
        }
        } catch (OptimisticLockException e) {
            log.warn("Optimistic lock exception for user {} during failed login handling. Concurrent update detected. Error: {}", usernameEmail, e.getMessage());
            // Decide on handling:
            // - Retry the operation (fetch user again, apply logic, save).
            // - Inform the user of a temporary issue.
            // - For login attempts, simply failing this attempt might be acceptable.
            //   The other concurrent operation likely succeeded or also failed.
            // For now, we'll just log it. The transaction will roll back.
        }
    }

    /**
     * Handles successful login attempts by resetting failed login counters and unlocking accounts.
     * Called by authentication success handler.
     * 
     * @param usernameEmail The username or email used in the successful login attempt
     */
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

    /**
     * Creates a new user account with the provided information.
     * Validates email uniqueness and password policy compliance.
     *
     * @param givenName User's first name
     * @param middleName User's middle name (optional)
     * @param surname User's last name
     * @param nickname User's preferred name (optional)
     * @param email User's primary email address
     * @param rawPassword User's password in plain text (will be hashed)
     * @param mobileNumber User's mobile phone number (optional)
     * @param defaultRegion Default region code for phone number formatting
     * @param recoveryEmailRaw Secondary email for account recovery (optional)
     * @return The newly created User entity
     * @throws DataIntegrityViolationException If email or recovery email already exists
     * @throws IllegalArgumentException If password policy is violated
     */
    @Transactional
    @Override
    @Timed(value = "authserver.usermanager.create", description = "Time taken to create user")
    public User createUser(String givenName, String middleName, String surname, String nickname,
                           String email, String rawPassword, String mobileNumber,
                           String defaultRegion, String recoveryEmailRaw) {
        log.info("Attempting to create user: email={}, recoveryEmail={}, givenName={}", email, recoveryEmailRaw, givenName);

        EmailAddress emailVO = new EmailAddress(email);
        EmailAddress recoveryEmailVO = StringUtils.hasText(recoveryEmailRaw) ? new EmailAddress(recoveryEmailRaw) : null; // Process recovery email

        if (recoveryEmailVO != null) {
            String recoveryBlindIndex = User.generateBlindIndex(recoveryEmailVO.getValue()); // Assuming generateBlindIndex is public static or accessible
            if (userRepository.existsByRecoveryEmailBlindIndex(recoveryBlindIndex)) {
                log.warn("Recovery email (via blind index) already exists for: {}", recoveryEmailRaw);
                throw new DataIntegrityViolationException("User with this recovery email already exists.");
            }
        }

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
        if (recoveryEmailVO != null && userRepository.existsByRecoveryEmailBlindIndex(recoveryEmailVO.getValue())) {
            log.warn("Recovery email {} already exists.", recoveryEmailVO.getValue());
            throw new DataIntegrityViolationException("User with this recovery email already exists.");
        }


        HashedPassword hashedPasswordVO = null;
        if (StringUtils.hasText(rawPassword)) { // Password can be optional for Passkey-first
            // Validation before hashing
            validatePasswordPolicy(rawPassword);
            hashedPasswordVO = new HashedPassword(passwordEncoder.encode(rawPassword));
            log.debug("Password provided, validated and will be hashed for new user: {}", email);
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
                .recoveryEmail(recoveryEmailVO) // This should trigger blind index generation in User.java
                .password(hashedPasswordVO) // Can be null
                .externalId(User.generateWebAuthnUserHandle()) // Generate external ID for WebAuthn credentials in a byte array. Must be generated as it needs to be populated.
                .mobileNumber(phoneNumberVO)
                .build();
        // The User.builder().build() or the setter for recoveryEmail in User.java
        // MUST ensure `recoveryEmailBlindIndex` is populated.
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
    @PreAuthorize("hasAuthority('PROFILE_EDIT_OWN')")
    @Timed(value = "authserver.usermanager.updateprofile", description = "Time taken to update user profile")
    public User updateUserProfile(Long userId, String newGivenName, String newMiddleName, String newSurname,
                                  String newNickname, String newMobileNumber, String defaultRegion,
                                  String newRecoveryEmailRaw) {
        log.info("Attempting to update profile for userId: {}. Updates: givenName={}, middleName={}, surname={}, nickname={}, mobileNumber={}, recoveryEmail={}",
                userId,
                (StringUtils.hasText(newGivenName) ? newGivenName : "[NO_CHANGE]"),
                (newMiddleName != null ? (newMiddleName.isEmpty() ? "[CLEAR]" : newMiddleName) : "[NO_CHANGE]"),
                (newSurname != null ? (newSurname.isEmpty() ? "[CLEAR]" : newSurname) : "[NO_CHANGE]"),
                (newNickname != null ? (newNickname.isEmpty() ? "[CLEAR]" : newNickname) : "[NO_CHANGE]"),
                (newMobileNumber != null ? newMobileNumber : "[NO_CHANGE]"),
                (newRecoveryEmailRaw != null ? (newRecoveryEmailRaw.isEmpty() ? "[CLEAR]" : newRecoveryEmailRaw) : "[NO_CHANGE]")
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
        if (newRecoveryEmailRaw != null) { // If an update attempt for recoveryEmail is made
            EmailAddress newRecoveryEmailVO = StringUtils.hasText(newRecoveryEmailRaw) ? new EmailAddress(newRecoveryEmailRaw) : null;
            String newRecoveryBlindIndex = (newRecoveryEmailVO != null) ? User.generateBlindIndex(newRecoveryEmailVO.getValue()) : null;

            // Check if the blind index actually changed to avoid unnecessary DB checks/updates
            if (!Objects.equals(user.getRecoveryEmailBlindIndex(), newRecoveryBlindIndex)) {
                // Check for conflict if new recovery email is not null and different from primary email
                if (newRecoveryEmailVO != null && newRecoveryEmailVO.equals(user.getEmail())) {
                    log.warn("Attempt to set recovery email same as primary email for userId: {}", userId);
                    throw new IllegalArgumentException("Recovery email cannot be the same as the primary email.");
                }
                // Check for conflict with other users' recovery emails if recoveryEmail is unique
                if (newRecoveryBlindIndex != null && userRepository.existsByRecoveryEmailBlindIndexAndIdNot(newRecoveryBlindIndex, userId)) {
                    log.warn("Recovery email {} already in use by another user (checked via blind index).", newRecoveryEmailRaw);
                    throw new DataIntegrityViolationException("This recovery email is already in use.");
                }
                user.updateRecoveryEmail(newRecoveryEmailVO); // This method in User should now also set the blind index
                log.debug("Updated recoveryEmail and its blind index for userId: {}", userId);
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
    public void changeUserPassword(Long userId, String oldRawPassword, String newRawPassword) {
        log.info("Attempting to change password for userId: {}", userId);
        // DO NOT log newRawPassword

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {} during password change attempt.", userId);
                    return new EntityNotFoundException("User not found with id: " + userId);
                });
        log.debug("Found user for password change: userId={}", userId);

        // Verify old password if the user is not an admin changing someone else's password
        // (This requires knowing who the caller is, or making this method strictly for self-service)
        // For self-service:
        if (user.getPassword() == null || !passwordEncoder.matches(oldRawPassword, user.getPassword())) {
            log.warn("Password change attempt for userId: {} failed. Old password did not match.", userId);
            // Consider throwing a specific BadCredentialsException or custom exception
            throw new PasswordMismatchException("Incorrect old password.");
        }

        // Validate new password before persisting
        validatePasswordPolicy(newRawPassword);

        HashedPassword newHashedPassword = new HashedPassword(passwordEncoder.encode(newRawPassword));
        user.changePassword(newHashedPassword);
        userRepository.save(user);
        log.info("Successfully changed password for userId: {}", userId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    @Timed(value = "authserver.usermanager.changepassword", description = "Time taken to admin change user password")
    @Override
    public void adminChangeUserPassword(Long userId, String newRawPassword) {
        log.info("Admin attempting to change password for userId: {}", userId);
        // DO NOT log newRawPassword

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {} during password change attempt by admin.", userId);
                    return new EntityNotFoundException("User not found with id: " + userId);
                });
        log.debug("Found user for password change by admin: userId={}", userId);

        // Verify if the admin is changing someone else's password through the admin api
        verifyAdminAccess("Password change attempt for userId: " + userId + " failed.");

        // Validate new password before persisting
        validatePasswordPolicy(newRawPassword);

        HashedPassword newHashedPassword = new HashedPassword(passwordEncoder.encode(newRawPassword));
        user.changePassword(newHashedPassword);
        userRepository.save(user);
        log.info("Admin successfully changed password for userId: {}", userId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    @Timed(value = "authserver.usermanager.changepassword", description = "Time taken to admin change user password")
    @Override
    public void adminChangeUserPasswordByUsername(String username, String newRawPassword) {
        log.info("Admin attempting to change password for username: {}", username);
        // DO NOT log newRawPassword

        User user = userRepository.findByEmail_ValueAndDeletedAtIsNull(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {} during password change attempt.", username);
                    return new EntityNotFoundException("User not found with username: " + username);
                });
        log.debug("Found user for password change: username={}", username);

        // Check if admin initiated change
        verifyAdminAccess("Password change attempt for username: " + username + " failed. Non-admin user initiated password change detected.");

        // Validate new password before persisting
        validatePasswordPolicy(newRawPassword);

        HashedPassword newHashedPassword = new HashedPassword(passwordEncoder.encode(newRawPassword));
        user.changePassword(newHashedPassword);
        userRepository.save(user);
        log.info("Admin successfully changed password for username: {}", username);
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

        // 0. Proactive session invalidation
        try {
            log.debug("Attempting to invalidate sessions for principalName: {}", principalName);
            Map<String, ? extends Session> userSessions = sessionRepository.findByPrincipalName(principalName);
            if (userSessions != null && !userSessions.isEmpty()) {
                int count = 0;
                for (String sessionId : userSessions.keySet()) {
                    sessionRepository.deleteById(sessionId);
                    count++;
                }
                log.info("Invalidated {} active session(s) for principalName: {}", count, principalName);
            } else {
                log.debug("No active sessions found to invalidate for principalName: {}", principalName);
            }
        } catch (Exception e) {
            log.error("Error during proactive session invalidation for principalName {}: {}", principalName, e.getMessage(), e);
        }

        // 1. Deleting associated PasskeyCredentials
        log.debug("Deleting passkey credentials for userId: {}", userId);
        passkeyCredentialRepository.deleteByUserId(userId);

        // 2. Deleting OAuth2Authorizations (invalidates access and refresh tokens)
        log.debug("Deleting OAuth2 authorizations for principalName: {}", principalName);
        oauth2AuthorizationRepository.deleteAllByPrincipalName(principalName);

        // 3. Deleting OAuth2AuthorizationConsents
        log.debug("Deleting OAuth2 authorization consents for principalName: {}", principalName);
        oauth2AuthorizationConsentRepository.deleteByIdPrincipalName(principalName);

        // Finally, deleting the User entity
        // This will also cascade delete related entities if configured in User (e.g., user_authorities join table)

        // 4. RememberMe - Remove/invalidate user token
        persistentTokenRepository.removeUserTokens(principalName);
        log.debug("Removed persistent tokens for user: {}", principalName);

        // 5. Soft delete the user entity
        user.setDeletedAt(Instant.now()); // Or set a boolean flag
        user.disableAccount();
        log.debug("Deleting user entity for userId: {}", userId);

        // This will trigger the @SQLDelete
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

    // --- Helper methods ---

    /**
     * Helper method to validate the password against the password policy.
     * This method should be called before persisting the password to the database.
     *
     * @param rawPassword The raw password as string to be validated before hashing.
     */
    /**
     * Validates the password against the password policy.
     * Checks length, character requirements, and if the password has been compromised.
     *
     * @param rawPassword The raw password as string to be validated before hashing
     * @throws IllegalArgumentException If the password doesn't meet policy requirements
     */
    private void validatePasswordPolicy(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        // Check minimum length
        if (rawPassword.length() < passwordPolicyConfig.getMinLength()) {
            throw new IllegalArgumentException("Password must be at least " + passwordPolicyConfig.getMinLength() + " characters long.");
        }

        // Check for uppercase letters using configured regex if available
        if (passwordPolicyConfig.isRequireUppercase()) {
            String regex = passwordPolicyConfig.getUppercaseRegex();
            if (regex != null && !regex.isEmpty()) {
                if (!Pattern.matches(regex, rawPassword)) {
                    throw new IllegalArgumentException("Password must contain an uppercase letter.");
                }
            } else if (!rawPassword.matches(".*[A-Z].*")) {
                throw new IllegalArgumentException("Password must contain an uppercase letter.");
            }
        }

        // Check for digits
        if (passwordPolicyConfig.isRequireDigit() && !rawPassword.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain a digit.");
        }

        // Check for special characters
        if (passwordPolicyConfig.isRequireSpecialChar() &&
                !rawPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain a special character.");
        }

        // Check if password has been compromised in a data breach
        if (compromisedPasswordChecker.check(rawPassword).isCompromised()) {
            throw new IllegalArgumentException("Password has been found in a data breach.");
        }

        log.debug("Password policy validation passed for the provided password.");
    }

    /**
     * Verifies that the current user has admin privileges.
     *
     * @param errorMessage The error message to use if verification fails
     * @throws AccessDeniedException if the current user doesn't have admin privileges
     */
    /**
     * Verifies that the current user has admin privileges.
     *
     * @param errorMessage The error message to use if verification fails
     * @throws AccessDeniedException if the current user doesn't have admin privileges
     */
    private void verifyAdminAccess(String errorMessage) {
        Authentication currentUserAuth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminInitiated = currentUserAuth != null &&
                currentUserAuth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority -> authority.equals("ROLE_ADMIN") ||
                                authority.equals("ROLE_SUPER_ADMIN") ||
                                authority.equals("ROLE_ROOT"));

        if (!isAdminInitiated) {
            log.warn(errorMessage);
            throw new AccessDeniedException("User without required privileges attempted to perform an admin operation.");
        } else {
            log.info("Admin operation initiated by: {}", currentUserAuth.getName());
        }
    }

    /**
     * Checks if a user has only regular user privileges (ROLE_USER) and no admin roles.
     *
     * @param userId The ID of the user to check
     * @return true if the user has only regular user privileges, false otherwise
     */
    public boolean isRegularUser(Long userId) {
        // Retrieve user from DB, check if they have ROLE_USER and NOT ADMIN/SUPER_ADMIN/ROOT_ADMIN
        // This is crucial to prevent admins from self-assigning higher roles.
        return hasRole(userId, "ROLE_USER") && !hasAnyRole(userId, "ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_ROOT");
    }

    /**
     * Checks if a user has admin privileges (ROLE_ADMIN).
     *
     * @param userId The ID of the user to check
     * @return true if the user has admin privileges, false otherwise
     */
    public boolean isAdmin(Long userId) {
        return hasRole(userId, "ROLE_ADMIN");
    }

    public boolean isSuperAdmin(Long userId) {
        return hasRole(userId, "ROLE_SUPER_ADMIN");
    }

    public boolean isRootAdmin(Long userId) {
        return hasRole(userId, "ROLE_ROOT");
    }

    public boolean isSuperAdminOrRootAdmin(Long userId) {
        return isSuperAdmin(userId) || isRootAdmin(userId);
    }

    /**
     * Checks if a user has a specific role.
     *
     * @param userId   The ID of the user to check
     * @param roleName The role name to check for
     * @return true if the user has the specified role, false otherwise
     */
    private boolean hasRole(Long userId, String roleName) {
        // Implement logic to query your database for the user's roles
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals(roleName));
    }

    /**
     * Checks if a user has any of the specified roles.
     *
     * @param userId    The ID of the user to check
     * @param roleNames The role names to check for
     * @return true if the user has any of the specified roles, false otherwise
     */
    private boolean hasAnyRole(Long userId, String... roleNames) {
        // Implement logic to check if user has any of the given roles
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        Set<String> userRoles = user.getAuthorities().stream()
                .map(Authority.class::cast)
                .map(Authority::getAuthority)
                .collect(Collectors.toSet());

        return Arrays.stream(roleNames).anyMatch(userRoles::contains);
    }
}

