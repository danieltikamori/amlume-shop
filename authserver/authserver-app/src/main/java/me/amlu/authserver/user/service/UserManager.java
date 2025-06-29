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
import me.amlu.authserver.exceptions.TooManyAttemptsException;
import me.amlu.authserver.exceptions.UnauthorizedException;
import me.amlu.authserver.oauth2.repository.OAuth2AuthorizationConsentRepository;
import me.amlu.authserver.oauth2.repository.OAuth2AuthorizationRepository;
import me.amlu.authserver.passkey.repository.PasskeyCredentialRepository;
import me.amlu.authserver.role.model.Role;
import me.amlu.authserver.role.repository.RoleRepository;
import me.amlu.authserver.security.audit.SecurityAuditService;
import me.amlu.authserver.security.config.PasswordPolicyConfig;
import me.amlu.authserver.security.dto.DeviceRegistrationInfo;
import me.amlu.authserver.security.failedlogin.FailedLoginAttemptService;
import me.amlu.authserver.security.repository.UserDeviceFingerprintRepository;
import me.amlu.authserver.security.service.CaptchaService;
import me.amlu.authserver.security.service.DeviceFingerprintServiceInterface;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.model.vo.HashedPassword;
import me.amlu.authserver.user.model.vo.PhoneNumber;
import me.amlu.authserver.user.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

import static me.amlu.authserver.common.SecurityConstants.MAX_PASSWORD_LENGTH;

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
    /**
     * Service for password encoding and verification
     */
    private final PasswordEncoder passwordEncoder;
    /**
     * Configuration for password policy rules
     */
    private final PasswordPolicyConfig passwordPolicyConfig;
    /**
     * Service to check if passwords have been compromised in data breaches
     */
    private final CompromisedPasswordChecker compromisedPasswordChecker;
    /**
     * Repository for user authority/role operations
     */
    private final RoleRepository roleRepository;
    /**
     * Repository for passkey credential operations
     */
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    /**
     * Repository for OAuth2 authorization operations
     */
    private final OAuth2AuthorizationRepository oauth2AuthorizationRepository;
    /**
     * Repository for OAuth2 consent operations
     */
    private final OAuth2AuthorizationConsentRepository oauth2AuthorizationConsentRepository;
    /**
     * Repository for persistent remember-me tokens
     */
    private final PersistentTokenRepository persistentTokenRepository;
    /**
     * Repository for user session management
     */
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    private final DeviceFingerprintServiceInterface deviceFingerprintService;
    private final FailedLoginAttemptService failedLoginAttemptService;
    private final SecurityAuditService securityAuditService;
    private final CaptchaService captchaService;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;


    /**
     * Constructs a UserManager with all required dependencies.
     *
     * @param userRepository                       Repository for user entity operations
     * @param passwordEncoder                      Service for password encoding and verification
     * @param passwordPolicyConfig                 Configuration for password policy rules
     * @param compromisedPasswordChecker           Service to check if passwords have been compromised
     * @param roleRepository                       Repository for user authority/role operations
     * @param passkeyCredentialRepository          Repository for passkey credential operations
     * @param oauth2AuthorizationRepository        Repository for OAuth2 authorization operations
     * @param oauth2AuthorizationConsentRepository Repository for OAuth2 consent operations
     * @param persistentTokenRepository            Repository for persistent remember-me tokens
     * @param sessionRepository                    Repository for user session management
     */
    public UserManager(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordPolicyConfig passwordPolicyConfig, CompromisedPasswordChecker compromisedPasswordChecker, RoleRepository roleRepository, PasskeyCredentialRepository passkeyCredentialRepository, OAuth2AuthorizationRepository oauth2AuthorizationRepository, OAuth2AuthorizationConsentRepository oauth2AuthorizationConsentRepository, PersistentTokenRepository persistentTokenRepository, FindByIndexNameSessionRepository<? extends Session> sessionRepository, DeviceFingerprintServiceInterface deviceFingerprintService, FailedLoginAttemptService failedLoginAttemptService, SecurityAuditService securityAuditService, CaptchaService captchaService, UserDeviceFingerprintRepository userDeviceFingerprintRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyConfig = passwordPolicyConfig;
        this.compromisedPasswordChecker = compromisedPasswordChecker;
        this.roleRepository = roleRepository;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.oauth2AuthorizationRepository = oauth2AuthorizationRepository;
        this.oauth2AuthorizationConsentRepository = oauth2AuthorizationConsentRepository;
        this.persistentTokenRepository = persistentTokenRepository;
        this.sessionRepository = sessionRepository;
        this.deviceFingerprintService = deviceFingerprintService;
        this.failedLoginAttemptService = failedLoginAttemptService;
        this.securityAuditService = securityAuditService;
        this.captchaService = captchaService;
        this.userDeviceFingerprintRepository = userDeviceFingerprintRepository;
    }

    /**
     * Handles failed login attempts by recording the failure and potentially locking the account.
     *
     * @param usernameEmail The username or email used in the failed login attempt
     */
    @Override
    @Timed(value = "authserver.usermanager.failedlogin", description = "Time taken to handle failed login attempt")
    public void handleFailedLogin(String usernameEmail) {
        if (usernameEmail == null || usernameEmail.trim().isEmpty()) {
            log.warn("Attempted to handle failed login with null or empty username");
            return;
        }

        log.debug("Handling failed login attempt for username/email: {}", usernameEmail);
        int retryCount = 0;
        final int MAX_RETRIES = 3;

        while (retryCount < MAX_RETRIES) {
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
                    return; // Success, exit the retry loop
                } else {
                    log.warn("User not found for failed login attempt: {}", usernameEmail);
                    return; // No user found, no need to retry
                }
            } catch (OptimisticLockException e) {
                retryCount++;
                log.warn("Optimistic lock exception for user {} during failed login handling (attempt {}/{}). Error: {}",
                        usernameEmail, retryCount, MAX_RETRIES, e.getMessage());

                if (retryCount >= MAX_RETRIES) {
                    log.error("Max retries reached for handling failed login for user: {}", usernameEmail);
                    // Consider alerting operations team if this happens frequently
                } else {
                    try {
                        Thread.sleep(50L * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Thread interrupted during retry backoff");
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error handling failed login for user {}: {}", usernameEmail, e.getMessage(), e);
                return; // Don't retry on unexpected errors
            }
        }
    }

    /**
     * Handles successful login attempts by resetting failed login counters, unlocking accounts,
     * and updating the device fingerprint.
     *
     * @param usernameEmail The username or email used in the successful login attempt.
     * @param deviceInfo    The DTO containing all device-related information.
     */
    @Override
    @Timed(value = "authserver.usermanager.successfullogin", description = "Time taken to handle successful login")
    public void handleSuccessfulLogin(String usernameEmail, DeviceRegistrationInfo deviceInfo) {
        log.debug("Handling successful login for username/email: {}", usernameEmail);
        userRepository.findByEmail_ValueAndDeletedAtIsNull(usernameEmail).ifPresent(user -> {
            boolean wasUpdated = false;

            // Reset login failures if necessary
            if (user.getAccountStatus().getFailedLoginAttempts() > 0 || !user.getAccountStatus().isAccountNonLocked()) {
                log.info("Resetting login failures for user {} (ID: {}).", usernameEmail, user.getId());
                user.recordSuccessfulLogin(); // This also resets login failures count
                wasUpdated = true;
            }

            // --- Update device fingerprint from the provided parameter ---
            if (deviceInfo != null && org.springframework.util.StringUtils.hasText(deviceInfo.deviceFingerprint())) {
                log.debug("Updating device fingerprint for user {}: {}", usernameEmail, deviceInfo.deviceFingerprint());

                // Update the current fingerprint in DeviceFingerprintingInfo (embedded object)
                user.getDeviceFingerprintingInfo().setCurrentFingerprint(deviceInfo.deviceFingerprint());

                // Delegate to the service's storeOrUpdateFingerprint method
                // This method will handle finding/updating/creating the UserDeviceFingerprint entity
                // Pass all collected metadata to the service
                deviceFingerprintService.storeOrUpdateFingerprint(user, deviceInfo);

                wasUpdated = true;
            }

            if (wasUpdated) {
                userRepository.save(user); // Save the User entity (which contains DeviceFingerprintingInfo)
                log.debug("Saved user {} (ID: {}) after successful login and updates.", usernameEmail, user.getId());
            }
        });
    }

    /**
     * Handles successful login attempts by resetting failed login counters and unlocking accounts.
     * Called by authentication success handler.
     *
     * @param usernameEmail The username or email used in the successful login attempt
     */
    @Deprecated
//    @Override
    @Timed(value = "authserver.usermanager.successfullogin", description = "Time taken to handle successful login")
    public void handleSuccessfulLogin(String usernameEmail) {
        log.debug("Handling successful login for username/email: {}", usernameEmail);
        userRepository.findByEmail_ValueAndDeletedAtIsNull(usernameEmail).ifPresent(user -> {
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
     * @param givenName        User's first name
     * @param middleName       User's middle name (optional)
     * @param surname          User's last name
     * @param nickname         User's preferred name (optional)
     * @param email            User's primary email address
     * @param rawPassword      User's password in plain text (will be hashed)
     * @param mobileNumber     User's mobile phone number (optional)
     * @param defaultRegion    Default region code for phone number formatting
     * @param recoveryEmailRaw Secondary email for account recovery (optional)
     * @return The newly created User entity
     * @throws DataIntegrityViolationException If email or recovery email already exists
     * @throws IllegalArgumentException        If password policy is violated
     */
    @Transactional
    @Timed(value = "authserver.usermanager.create", description = "Time taken to create user")
    @Override
    public User createUser(String givenName, String middleName, String surname, String nickname,
                           String email, String rawPassword, String mobileNumber,
                           String defaultRegion, String recoveryEmailRaw, String captchaResponse, String ipAddress) {
        log.info("Attempting to create user: email={}, recoveryEmail={}, givenName={}", email, recoveryEmailRaw, givenName);

        // 0. Perform local pre-flight checks (rate limiting, captcha)
        // Keep these if amlume-shop should enforce them before hitting authserver
        // Use userEmail as the identifier for local rate limiting
        // Ensure request.userEmail() and request.userEmail().getEmail() are not null before calling
        String emailForPreCheck = (!email.isEmpty())
                ? email
                : "unknown_email_for_precheck"; // Or handle as error
        performPreFlightChecks(emailForPreCheck, ipAddress, "Registration", captchaResponse);
        log.debug("Local pre-flight checks passed for registration of user userEmail: {}", emailForPreCheck);

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

        roleRepository.findByName("ROLE_USER").ifPresentOrElse(
                newUser::assignRole,
                () -> log.warn("Default role 'ROLE_USER' not found. New user {} will have no roles.", email)
        );
        log.debug("Assigned default roles (if found) to new user: {}", email);

        User savedUser = userRepository.save(newUser);
        log.info("Successfully created and saved user: email={}, recoveryEmail={}, userId={}",
                savedUser.getEmail().getValue(),
                (savedUser.getRecoveryEmail() != null ? savedUser.getRecoveryEmail().getValue() : "N/A"),
                savedUser.getId());
        return savedUser;
    }

    @Transactional
    @Override
    @PreAuthorize("hasAuthority('PROFILE_EDIT_OWN') and @userSecurity.isUserAllowedToModify(#userId)")
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
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN') and @userSecurity.isUserAllowedToModify(#userId)")
    @Timed(value = "authserver.usermanager.changepassword", description = "Time taken to change user password")
    public void changeUserPassword(Long userId, String oldRawPassword, String newRawPassword) {
        log.info("Attempting to change password for userId: {}", userId);
        // DO NOT log newRawPassword

        // Prevent password reuse
        if (oldRawPassword != null && oldRawPassword.equals(newRawPassword)) {
            log.warn("Password change attempt for userId: {} failed. New password same as old password.", userId);
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

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

        try {
            // Validate new password before persisting
            validatePasswordPolicy(newRawPassword);

            HashedPassword newHashedPassword = new HashedPassword(passwordEncoder.encode(newRawPassword));
            user.changePassword(newHashedPassword);
            userRepository.save(user);

            // Invalidate existing sessions for security
            try {
                String principalName = user.getUsername();
                Map<String, ? extends Session> userSessions = sessionRepository.findByPrincipalName(principalName);
                if (userSessions != null && !userSessions.isEmpty()) {
                    // Keep current session if applicable
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    String currentSessionId = null;
                    if (auth != null && auth.getDetails() instanceof Map) {
                        currentSessionId = (String) ((Map<?, ?>) auth.getDetails()).get("sessionId");
                    }

                    for (String sessionId : userSessions.keySet()) {
                        if (!sessionId.equals(currentSessionId)) {
                            sessionRepository.deleteById(sessionId);
                        }
                    }
                    log.debug("Invalidated other sessions after password change for userId: {}", userId);
                }
            } catch (Exception e) {
                log.warn("Failed to invalidate sessions after password change: {}", e.getMessage());
                // Continue - password was changed successfully
            }

            log.info("Successfully changed password for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error during password change for userId {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class})
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

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ROLE_USER') and @userSecurity.isUserAllowedToModify(#userId)")
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

        // Proactive session invalidation
        invalidateAllSessions(userId);

        // Deleting associated PasskeyCredentials
        log.debug("Deleting passkey credentials for userId: {}", userId);
        passkeyCredentialRepository.deleteByUserId(userId);

        // Deleting associated UserDeviceFingerprints
        log.debug("Deleting user device fingerprints for userId: {}", userId);
        userDeviceFingerprintRepository.deleteByUserId(userId);

        // Deleting OAuth2Authorizations (invalidates access and refresh tokens)
        log.debug("Deleting OAuth2 authorizations for principalName: {}", principalName);
        oauth2AuthorizationRepository.deleteAllByPrincipalName(principalName);

        // Deleting OAuth2AuthorizationConsents
        log.debug("Deleting OAuth2 authorization consents for principalName: {}", principalName);
        oauth2AuthorizationConsentRepository.deleteByIdPrincipalName(principalName);

        // Finally, deleting the User entity
        // This will also cascade delete related entities if configured in User (e.g., user_authorities join table)

        // RememberMe - Remove/invalidate user token
        persistentTokenRepository.removeUserTokens(principalName);
        log.debug("Removed persistent tokens for user: {}", principalName);

        // Soft delete the user entity
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

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    public void appendRole(@NonNull Long userId, @NonNull String roleName) {
        log.info("Attempting to append role {} to user {}", roleName, userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User not found with id: {}", userId);
            return;
        }
        log.debug("Found user for role append: userId={}", userId);

        // Assign role to user
        roleRepository.findByName(roleName).ifPresentOrElse(
                user::assignRole,
                () -> log.warn("Role '{}' not found for assignment to user {}.", roleName, userId)
        );

        log.debug("Assigned role {} to user {}", roleName, userId);

        String principalName = user.getUsername();

        // For security purposes, all sessions should be invalidated
        // Proactive session invalidation
        invalidateAllSessions(userId);

        // Update authorizations

        // Deleting OAuth2Authorizations (invalidates access and refresh tokens)
        log.debug("Deleting OAuth2 authorizations for principalName: {}", principalName);
        oauth2AuthorizationRepository.deleteAllByPrincipalName(principalName);

        // Deleting OAuth2AuthorizationConsents
        log.debug("Deleting OAuth2 authorization consents for principalName: {}", principalName);
        oauth2AuthorizationConsentRepository.deleteByIdPrincipalName(principalName);

        // RememberMe - Remove/invalidate user token
        persistentTokenRepository.removeUserTokens(principalName);
        log.debug("Removed persistent tokens for user: {}", principalName);

        // RememberMe - Remove/invalidate user token
        persistentTokenRepository.removeUserTokens(principalName);
        log.debug("Removed persistent tokens for user as role(s) were appended: {}", principalName);

        userRepository.save(user);
        log.info("Successfully appended role {} to user {}", roleName, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    public void revokeRole(@NonNull Long userId, @NonNull String roleName) {
        log.info("Attempting to remove role {} from user {}", roleName, userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User not found with id: {}", userId);
            return;
        }
        log.debug("Found user for role removal: userId={}", userId);
        roleRepository.findByName(roleName).ifPresentOrElse(
                user::revokeRole,
                () -> log.warn("Role '{}' not found for revocation from user {}.", roleName, userId)
        );

        log.debug("Revoked role {} from user {}", roleName, userId);

        String principalName = user.getUsername();

        // For security purposes, all sessions should be invalidated
        // Proactive session invalidation

        invalidateAllSessions(userId);

        // Update authorizations

        // Deleting OAuth2Authorizations (invalidates access and refresh tokens)
        log.debug("Deleting OAuth2 authorizations for principalName: {}", principalName);
        oauth2AuthorizationRepository.deleteAllByPrincipalName(principalName);

        // Deleting OAuth2AuthorizationConsents
        log.debug("Deleting OAuth2 authorization consents for principalName: {}", principalName);
        oauth2AuthorizationConsentRepository.deleteByIdPrincipalName(principalName);

        // RememberMe - Remove/invalidate user token
        persistentTokenRepository.removeUserTokens(principalName);
        log.debug("Removed persistent tokens for user: {}", principalName);


        userRepository.save(user);
        log.info("Successfully removed role {} from user {}", roleName, userId);
    }

    /**
     * Retrieves the currently authenticated user from the SecurityContext.
     *
     * @return The authenticated User entity.
     * @throws UnauthorizedException If no user is authenticated or the user is anonymous.
     */
    @Override
    // Caching the current user can be tricky due to keying and potential staleness.
    // Often better to fetch when necessary or cache specific, less volatile data.
    // @Cacheable(value = CURRENT_USER_CACHE, key = "authentication.name") // Key needs context
    @Timed(value = "authserver.userservice.currentuser", extraTags = {"method", "getCurrentUser"}, description = "Time taken to get current user")
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("No authenticated user found or user is anonymous");
        }

        // Action: Handle OAuth2/OIDC principal correctly
        Object principal = authentication.getPrincipal();

        if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            // For JWT principal, the 'sub' claim is the user ID from authserver.
            String authServerSubjectId = jwt.getSubject(); // This is the authserver User.id (as a String)
            // The 'userEmail' claim might also be present and useful for logging/display
            String emailFromToken = jwt.getClaimAsString("userEmail"); // Assuming authserver adds userEmail claim

            log.debug("Authenticated via JWT. Subject (authserver user ID): {}, Email from token: {}", authServerSubjectId, emailFromToken);

            // Action: Find the local amlume-shop User entity using the authServerSubjectId
            // You need a new repository method: findByAuthServerSubjectId(String subjectId)
            // And the authServerSubjectId field in amlume-shop.User entity.
            User currentUser = userRepository.findByEmail_ValueAndDeletedAtIsNull(authServerSubjectId)
                    .orElseThrow(() -> {
                        log.error("Local user not found for authserver subject ID: {}", authServerSubjectId);
                        // This indicates a provisioning issue - the user logged in via authserver
                        // but no corresponding local user record was created/linked.
                        // This should be handled by the OidcUserService/OAuth2UserService.
                        // Throwing UnauthorizedException or a specific provisioning error might be appropriate.
                        return new UnauthorizedException("Local user profile not found for authenticated identity.");
                    });

            // Optional: Update local user data from token claims if needed (e.g., name, userEmail)
            // This logic might be better placed in the OidcUserService/OAuth2UserService during provisioning/loading.
            // if (emailFromToken != null && !emailFromToken.equals(currentUser.getContactInfo().getEmail())) {
            //     currentUser.updateContactInfo(currentUser.getContactInfo().withEmail(emailFromToken));
            //     userRepository.save(currentUser); // Save the updated local user
            // }

            log.debug("Successfully retrieved local user for authserver subject ID {}: userId={}", authServerSubjectId, currentUser.getUserId());
            return currentUser;

        } else if (principal instanceof UserDetails userDetails) {
            // This branch handles cases where the principal is already a UserDetails object,
            // which might happen with local form login (if still enabled) or other custom auth.
            // If local form login is removed, this branch might become less relevant or indicate an issue.
            // If UserDetails is your amlume-shop.User entity, return it directly.
            if (userDetails instanceof User localUser) {
                log.debug("Authenticated via UserDetails principal: username={}", userDetails.getUsername());
                return localUser;
            } else {
                // Handle other UserDetails types if necessary, or throw error
                log.error("Authenticated with unexpected UserDetails type: {}", userDetails.getClass());
                throw new UnauthorizedException("Unexpected authenticated principal type.");
            }
        }
        // Remove or adapt other principal types if they were handled here (e.g., old PASETO principals)

        else {
            // Should not happen with standard Spring Security setup after OAuth2 config
            log.error("Unexpected principal type in SecurityContext: {}", principal.getClass());
            throw new UnauthorizedException("Unexpected authentication principal type");
        }
    }


    // --- Helper methods ---

    /**
     * Consolidated pre-flight checks for registration and login.
     * // The 'identifier' parameter will now be an userEmail address.
     * Keep these if amlume-shop should enforce them before hitting authserver/login flow.
     */
    private void performPreFlightChecks(String identifier, String ipAddress, String actionType, String captchaResponse) throws TooManyAttemptsException {

        // Check failed login attempts (applies to log in/MFA verify, maybe registration too)
        // This logic might need adjustment if authserver handles all failed attempts centrally.
        // If amlume-shop still wants to block based on *local* failed attempts (e.g., attempts to hit its login endpoint directly), keep this.
        // If authserver handles it, remove this.
        // For now, assuming local IP/username rate limiting is still desired before calling authserver.
        try {
            failedLoginAttemptService.checkAndThrowIfBlocked(identifier); // Check by username/userEmail
            failedLoginAttemptService.checkAndThrowIfBlocked(ipAddress); // Check by IP
        } catch (TooManyAttemptsException e) {
            // Use the exception message which likely contains the key type information
            securityAuditService.logFailedAttempt(identifier, ipAddress, "Too Many Attempts (Backoff) for " + e.getMessage());
            throw e;
        }

        // Keep Captcha validation here if required for the actionType (e.g., on amlume-shop's registration form)
        if ("Login".equals(actionType) || "Registration".equals(actionType)) {
            captchaService.verifyRateLimitAndCaptcha(captchaResponse, ipAddress);
        }

        log.trace("Local pre-flight checks passed for identifier [{}], action [{}]", identifier, actionType);
    }

    /**
     * Validates the password against the password policy.
     * Checks length, character requirements, and if the password has been compromised.
     * This method should be called before persisting the password to the database.
     *
     * @param rawPassword The raw password as string to be validated before hashing
     * @throws IllegalArgumentException If the password doesn't meet policy requirements
     */
    private void validatePasswordPolicy(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        // Prevent extremely long passwords that could cause DoS
        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password is too long (maximum " + MAX_PASSWORD_LENGTH + " characters).");
        }

        // Check minimum length
        if (rawPassword.length() < passwordPolicyConfig.getMinLength()) {
            throw new IllegalArgumentException("Password must be at least " + passwordPolicyConfig.getMinLength() + " characters long.");
        }

        // Check for uppercase letters using configured regex if available
        if (passwordPolicyConfig.isRequireUppercase()) {
            String regex = passwordPolicyConfig.getUppercaseRegex();
            if (regex != null && !regex.isEmpty()) {
                try {
                    if (!Pattern.matches(regex, rawPassword)) {
                        throw new IllegalArgumentException("Password must contain an uppercase letter.");
                    }
                } catch (Exception e) {
                    log.error("Invalid uppercase regex pattern: {}", regex, e);
                    // Fallback to default pattern
                    if (!rawPassword.matches(".*[A-Z].*")) {
                        throw new IllegalArgumentException("Password must contain an uppercase letter.");
                    }
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

        try {
            // Check if password has been compromised in a data breach
            if (compromisedPasswordChecker.check(rawPassword).isCompromised()) {
                throw new IllegalArgumentException("Password has been found in a data breach.");
            }
        } catch (Exception e) {
            log.warn("Unable to check if password is compromised: {}", e.getMessage());
            // Continue without breach check if service is unavailable
        }

        log.debug("Password policy validation passed for the provided password.");
    }

    /**
     * Verifies that the current user has admin privileges.
     *
     * @param errorMessage The error message to use if verification fails
     * @throws AccessDeniedException if the current user doesn't have admin privileges
     */
    private void verifyAdminAccess(String errorMessage) {
        Authentication currentUserAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentUserAuth == null) {
            log.warn("No authentication found when verifying admin access");
            throw new AccessDeniedException("Authentication required for admin operations");
        }

        boolean isAdminInitiated = currentUserAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // Authority is the Role.name
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") ||
                        authority.equals("ROLE_SUPER_ADMIN") ||
                        authority.equals("ROLE_ROOT"));

        if (!isAdminInitiated) {
            log.warn(errorMessage);
            throw new AccessDeniedException("User without required privileges attempted to perform an admin operation.");
        } else {
            // Audit log for admin operations
            log.info("Admin operation initiated by: {} with roles: {}",
                    currentUserAuth.getName(),
                    currentUserAuth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(", ")));
        }
    }

    public void invalidateAllSessions(Long userId) {
        log.info("Attempting to invalidate all sessions for userId: {}", userId);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User not found with id for session invalidation: {}", userId);
            return;
        }
        log.debug("Found user for session invalidation: userId={}", userId);

        String principalName = user.getUsername();
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
     * @return true if the user has admin privileges, false otherwise.
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
     * @return true if the user has the specified role, false otherwise.
     */
    private boolean hasRole(Long userId, String roleName) {
        // Implement logic to query your database for the user's roles
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName)); // <-- Check against Role.name
//        return user != null && user.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals(roleName));
    }

    /**
     * Checks if a user has any of the specified roles.
     *
     * @param userId    The ID of the user to check
     * @param roleNames The role names to check for
     * @return true if the user has any of the specified roles, false otherwise.
     */
    private boolean hasAnyRole(Long userId, String... roleNames) {
        // Implement logic to check if user has any of the given roles
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        Set<String> userRoleNames = user.getRoles().stream()
                .map(Role::getName) // <-- Map to Role.name
                .collect(Collectors.toSet());
        return Arrays.stream(roleNames).anyMatch(userRoleNames::contains);

//        Set<String> userRoles = user.getAuthorities().stream()
//                .map(Role.class::cast)
//                .map(Role::getAuthority)
//                .collect(Collectors.toSet());
//
//        return Arrays.stream(roleNames).anyMatch(userRoles::contains);
    }
}
