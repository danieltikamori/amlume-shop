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

// Removed Constants import as MAX_FAILED_ATTEMPTS check is now fully delegated
// import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.MfaException;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.exceptions.UserLockedException;
import me.amlu.shop.amlume_shop.exceptions.UserNotFoundException;
import me.amlu.shop.amlume_shop.model.MfaSettings;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.ratelimiter.RateLimiter;
import me.amlu.shop.amlume_shop.security.aspect.RequiresAuthentication;
import me.amlu.shop.amlume_shop.security.aspect.RequiresRole;
import me.amlu.shop.amlume_shop.user_management.*;
import me.amlu.shop.amlume_shop.repositories.MfaSettingsRepository;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants.DEFAULT_ISSUER;
import static me.amlu.shop.amlume_shop.user_management.AppRole.ROLE_ADMIN;
import static me.amlu.shop.amlume_shop.user_management.AppRole.ROLE_SUPER_ADMIN;
import static me.amlu.shop.amlume_shop.user_management.AppRole.ROLE_ROOT;


@Service
public class MfaServiceImpl implements MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaServiceImpl.class);

    private final MfaSettingsRepository mfaSettingsRepository;
    private final MfaTokenRepository mfaTokenRepository;
    private final TOTPService totpService;
    private final UserService userService;
    private final RateLimiter rateLimiter;
    private final UserRepository userRepository;

    public MfaServiceImpl(MfaSettingsRepository mfaSettingsRepository,
                          MfaTokenRepository mfaTokenRepository,
                          TOTPService totpService,
                          UserService userService,
                          @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter, UserRepository userRepository
    ) {
        this.mfaSettingsRepository = mfaSettingsRepository;
        this.mfaTokenRepository = mfaTokenRepository;
        this.totpService = totpService;
        this.userService = userService;
        this.rateLimiter = rateLimiter;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMfaEnabled(User user) {
        if (user == null) {
            log.warn("isMfaEnabled called with null user.");
            return false;
        }
        if (user.getMfaInfo() != null && user.getMfaInfo().isMfaEnabled()) {
            boolean tokenExists = mfaTokenRepository.findByUser(user).isPresent();
            if (!tokenExists) {
                log.warn("User entity indicates MFA enabled, but no MfaToken found in repository for user ID: {}", user.getUserId());
                // Consider if this state should automatically disable MFA or throw an error
            }
            // Return true based on User entity state, even if the token is missing (might be during setup)
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMfaEnforced(User user) {
        // Check global setting first
        Optional<MfaSettings> settings = mfaSettingsRepository.findById(1L);
        if (settings.isPresent() && settings.get().isMfaEnforced()) {
            return true; // Globally enforced
        }
        // Check user-specific setting if global is not enforced (or doesn't exist)
        return user != null && user.getMfaInfo() != null && user.getMfaInfo().isMfaEnforced();
    }

    @Override
    @Transactional
    @RequiresAuthentication // Assuming only authenticated admins can change this
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true) // Restrict to ADMIN role
    public void updateMfaEnforced(boolean enforced) {
        MfaSettings settings = mfaSettingsRepository.findById(1L)
                .orElse(new MfaSettings());
        settings.setId(1L); // Ensure ID is set for new or existing entity
        settings.setMfaEnforced(enforced);
        mfaSettingsRepository.save(settings);
        log.info("Global MFA enforcement updated to: {}", enforced);
    }

    @Override
    public String generateSecretKey() {
        return totpService.generateSecretKey();
    }

    @Override
    public String generateQrCodeImageUrl(User user, String encryptedSecret) {
        if (user == null || user.getUsername() == null || encryptedSecret == null) {
            log.error("Cannot generate QR code URL with null user, username, or secret.");
            throw new IllegalArgumentException("User, username, and secret must not be null for QR code generation.");
        }
        // Consider using a more specific issuer name if applicable
        return totpService.generateQrCodeUrl(user, encryptedSecret, DEFAULT_ISSUER);
    }

    /**
     * Records a failed MFA attempt by delegating to UserService.
     * UserService handles incrementing the count and potentially locking the account.
     * @param user The user who failed the attempt.
     */
    @Override
    @Transactional // Modifies user state via UserService
    public void recordFailedAttempt(User user) {
        if (user == null || user.getUsername() == null) {
            log.warn("Attempted to record failed MFA attempt for null user or user with null username.");
            return;
        }
        log.warn("Recording failed MFA attempt for user: {}", user.getUsername());
        try {
            // Delegate to UserService which handles incrementing and locking logic
            userService.incrementFailedLogins(user.getUsername());
        } catch (UserNotFoundException e) {
            log.error("User {} not found when trying to record failed MFA attempt.", user.getUsername(), e);
            // Potentially re-throw or handle as an authentication error
            throw new MfaException(MfaException.MfaErrorType.AUTHENTICATION_ERROR, "User not found during MFA failure.", e);
        } catch (Exception e) {
            // Catch potential exceptions from incrementFailedLogins (like DB issues)
            log.error("Error incrementing failed login attempts for user {} during MFA failure.", user.getUsername(), e);
            // Consider re-throwing a specific exception if needed
            throw new MfaException(MfaException.MfaErrorType.AUTHENTICATION_ERROR, "Failed to record MFA attempt", e);
        }
    }

    /**
     * Checks if the user account is currently locked by reading the user's state.
     * @param user The user to check.
     * @return true if the account is locked, false otherwise.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isUserLocked(User user) {
        if (user == null) {
            log.warn("isUserLocked called with null user.");
            return true; // Fail safe: treat null user as locked
        }
        // Check the user's account status directly from the entity
        boolean locked = !user.isAccountNonLocked();
        if (locked) {
            log.warn("User account is locked: {}", user.getUsername());
        }
        return locked;
    }

    /**
     * Resets the failed login attempt count for the user by delegating to UserService.
     * @param user The user whose attempts should be reset.
     */
    @Override
    @Transactional // Modifies user state via UserService
    public void resetFailedAttempts(User user) {
        if (user == null || user.getUserId() == null) {
            log.warn("Attempted to reset failed MFA attempts for null user or user with null ID.");
            return;
        }
        log.info("Resetting failed login attempts for user: {}", user.getUsername());
        try {
            userService.resetFailedLoginAttempts(user.getUserId());
        } catch (Exception e) {
            log.error("Error resetting failed login attempts for user {}.", user.getUsername(), e);
            // Consider re-throwing or handling more robustly
            throw new MfaException(MfaException.MfaErrorType.AUTHENTICATION_ERROR, "Failed to reset MFA attempts", e);
        }
    }

    // --- REMOVED REDUNDANT METHODS ---
    /* Methods like shouldLockAccount and lockAccount were removed as this logic
       is now correctly delegated to and handled by the UserService. */
    // --- END REMOVED REDUNDANT METHODS ---

    @Override
    @Transactional // Reads user state, potentially modifies via recordFailedAttempt/resetFailedAttempts
    public boolean verifyCode(String secret, String code) throws TooManyAttemptsException, MfaException {
        User user;
        try {
            // Assuming verifyCode is called within an authenticated context where MFA is needed
            user = userService.getCurrentUser();
        } catch (UserNotFoundException | IllegalStateException e) {
            log.error("Could not retrieve current user during MFA verification.", e);
            throw new MfaException(MfaException.MfaErrorType.AUTHENTICATION_ERROR, "User not authenticated or found.", e);
        }

        // 1. Check rate limit first to prevent unnecessary processing/locking
        if (isRateLimitExceeded(user)) {
            log.warn("MFA verification rate limit exceeded for user: {}", user.getUsername());
            // Do NOT record failed attempt here, as it's a rate limit issue, not necessarily a wrong code
            throw new TooManyAttemptsException("Rate limit exceeded for MFA verification.");
        }

        // 2. Check if the user account is already locked
        if (isUserLocked(user)) {
            log.warn("MFA verification attempted for locked user: {}", user.getUsername());
            // Do NOT record another failed attempt if already locked
            throw new UserLockedException("Account is locked.");
        }

        boolean isValid;
        try {
            // 3. Verify the code using TOTPService
            isValid = totpService.verifyCode(secret, code);

            if (!isValid) {
                log.warn("Invalid MFA code provided for user: {}", user.getUsername());
                // 4a. Record the failed attempt (delegates to UserService, which handles locking)
                recordFailedAttempt(user); // This might throw MfaException if recording fails

                // 4b. **Re-check** if the user is now locked *after* recording the failure
                // Fetch a potentially updated user state. Use findUserByUsername for consistency.
                User potentiallyUpdatedUser = userService.findUserByUsername(user.getUsername());
                if (isUserLocked(potentiallyUpdatedUser)) {
                    log.warn("User account {} became locked due to this failed MFA attempt.", user.getUsername());
                    // Throw UserLockedException specifically if the lock happened *because* of this attempt
                    throw new UserLockedException("Account locked due to too many failed MFA attempts.");
                } else {
                    // If not locked, just throw invalid code exception
                    throw new MfaException(MfaException.MfaErrorType.INVALID_CODE, "Invalid MFA code provided.");
                }
            } else {
                // 5. Code is valid, reset failed attempts
                log.debug("MFA code verified successfully for user: {}", user.getUsername());
                resetFailedAttempts(user); // Delegate reset to UserService
                return true; // Code is valid
            }

        } catch (UserLockedException | MfaException | TooManyAttemptsException e) {
            // Re-throw exceptions we expect and handle specifically
            throw e;
        } catch (Exception e) { // Catch unexpected errors (e.g., from TOTPService, UserService)
            log.error("Unexpected error during MFA code verification for user: {}", user.getUsername(), e);
            // Avoid recording failed attempt here unless sure it's appropriate
            throw new MfaException(MfaException.MfaErrorType.VERIFICATION_ERROR, "Error verifying MFA code.", e);
        }
    }

    @Override
    @Transactional
    @RequiresAuthentication // User must be authenticated to enable MFA for themselves
    public void enableMfaForUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        // Re-fetch user within transaction to ensure fresh state
        User managedUser = userService.getUserById(user.getUserId());

        if (isMfaEnabled(managedUser)) {
            log.info("MFA is already enabled for user: {}", managedUser.getUsername());
            return;
        }

        String secret = generateSecretKey();
        // Create and save the MfaToken entity
        MfaToken mfaToken = MfaToken.builder()
                .user(managedUser)
                .secret(secret) // Store the raw secret (consider encryption if required by policy)
                .enabled(true) // Mark as enabled
                .build();
        mfaTokenRepository.save(mfaToken);

        // Update User entity's MfaInfo embeddable
        MfaInfo currentMfaInfo = managedUser.getMfaInfo();
        MfaInfo updatedMfaInfo = new MfaInfo(
                true, // Set enabled to true
                currentMfaInfo != null && currentMfaInfo.isMfaEnforced(), // Keep enforced status
                new MfaQrCodeUrl(generateQrCodeImageUrl(managedUser, secret)), // Generate and store QR URL
                new MfaSecret(secret) // Store secret in embeddable (consider if needed alongside MfaToken)
        );
        managedUser.updateMfaInfo(updatedMfaInfo);

        // Persist user changes directly via repository within the same transaction
        try {
//            userService.updateUserProfile(managedUser.getUserId(), null); // Use existing service method if it handles null DTO for internal updates
             userRepository.save(managedUser); // Or save directly if UserService doesn't have a suitable method
            log.info("MFA enabled successfully for user: {}", managedUser.getUsername());
        } catch (Exception e) {
            log.error("Failed to persist User entity after enabling MFA for user {}. MFA token saved, but User entity might be inconsistent.", managedUser.getUsername(), e);
            // Rollback should happen due to @Transactional
            throw new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "Failed to update user profile after enabling MFA", e);
        }
    }

    @Override
    @Transactional
    @RequiresAuthentication // User must be authenticated to disable MFA for themselves
    public void disableMfaForUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        // Re-fetch user within transaction
        User managedUser = userService.getUserById(user.getUserId());

        // Delete the MfaToken entity if it exists
        mfaTokenRepository.findByUser(managedUser).ifPresentOrElse(
                token -> {
                    mfaTokenRepository.delete(token);
                    log.info("MFA token deleted for user: {}", managedUser.getUsername());
                },
                () -> log.info("No MFA token found to delete for user: {}", managedUser.getUsername())
        );

        // Update User entity's MfaInfo embeddable
        MfaInfo currentMfaInfo = managedUser.getMfaInfo();
        if (currentMfaInfo != null && currentMfaInfo.isMfaEnabled()) {
            // Create a new MfaInfo instance with MFA disabled
            MfaInfo updatedMfaInfo = new MfaInfo(
                    false, // Set enabled to false
                    currentMfaInfo.isMfaEnforced(), // Keep enforced status
                    null, // Clear QR code URL
                    null  // Clear secret
            );
            managedUser.updateMfaInfo(updatedMfaInfo);

            // Persist user changes directly via repository
            try {
//                userService.updateUserProfile(managedUser.getUserId(), null); // Use existing service method if it handles null DTO
                 userRepository.save(managedUser); // Or save directly
                log.info("MFA disabled in User entity for user: {}", managedUser.getUsername());
            } catch (Exception e) {
                log.error("Failed to persist User entity after disabling MFA for user {}. MFA token deleted, but User entity might be inconsistent.", managedUser.getUsername(), e);
                // Rollback should happen due to @Transactional
                throw new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "Failed to update user profile after disabling MFA", e);
            }
        } else {
            log.info("MFA was already disabled or MfaInfo was null for user: {}", managedUser.getUsername());
        }
    }

    // --- Administrative Methods ---

    @Override
    @Transactional
    @RequiresAuthentication // Requires authentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true) // Requires ADMIN role
    public void resetMfaForUser(User user) {
        // Changed log level from warn to info as this is an intended administrative action
        log.info("Resetting MFA for user: {}. Delegating to disableMfaForUser.", user != null ? user.getUsername() : "null");
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for MFA reset.");
        }
        // Re-fetch user to ensure it's managed
        User managedUser = userService.getUserById(user.getUserId());
        disableMfaForUser(managedUser); // The simplest reset is disabling
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void updateSecretForUser(User user, String encryptedSecret) {
        if (user == null || encryptedSecret == null) {
            throw new IllegalArgumentException("User and secret cannot be null");
        }
        // Re-fetch user
        User managedUser = userService.getUserById(user.getUserId());
        log.info("Updating MFA secret for user: {}", managedUser.getUsername()); // Changed to info

        MfaToken mfaToken = mfaTokenRepository.findByUser(managedUser)
                .orElseThrow(() -> new MfaException(MfaException.MfaErrorType.TOKEN_NOT_FOUND, "MFA not enabled or token not found for user"));

        mfaToken.setSecret(encryptedSecret); // Update secret in token entity
        mfaTokenRepository.save(mfaToken);

        // Update User entity's MfaInfo embeddable
        MfaInfo currentMfaInfo = managedUser.getMfaInfo();
        if (currentMfaInfo != null) {
            MfaInfo updatedMfaInfo = new MfaInfo(
                    currentMfaInfo.isMfaEnabled(), // Keep enabled status
                    currentMfaInfo.isMfaEnforced(), // Keep enforced status
                    new MfaQrCodeUrl(generateQrCodeImageUrl(managedUser, encryptedSecret)), // Update QR code URL
                    new MfaSecret(encryptedSecret) // Update secret in embeddable
            );
            managedUser.updateMfaInfo(updatedMfaInfo);

            // Persist user changes
            try {
//                userService.updateUserProfile(managedUser.getUserId(), null); // Use existing service method
                 userRepository.save(managedUser); // Or save directly
                log.info("MFA secret updated successfully for user: {}", managedUser.getUsername());
            } catch (Exception e) {
                log.error("Failed to persist User entity after updating MFA secret for user {}.", managedUser.getUsername(), e);
                throw new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "Failed to update user profile after updating MFA secret", e);
            }
        } else {
            log.error("MfaInfo is null for user {} during secret update.", managedUser.getUsername());
            // This indicates an inconsistent state, should ideally roll back
            throw new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "User MfaInfo is missing.");
        }
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteMfaTokenForUser(User user) {

        log.info("Method deleteMfaTokenForUser called. Delegating to disableMfaForUser.");
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for MFA token deletion.");
        }
        User managedUser = userService.getUserById(user.getUserId());
        disableMfaForUser(managedUser); // Disabling handles token deletion and user update
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteSecretForUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        User managedUser = userService.getUserById(user.getUserId());

        log.info("Deleting MFA secret for user: {}", managedUser.getUsername());

        // Delete the token entity entirely, as a secret without a token is inconsistent
        mfaTokenRepository.findByUser(managedUser).ifPresent(mfaTokenRepository::delete);

        // Update User entity's MfaInfo to remove secret (effectively disabling)
        MfaInfo currentMfaInfo = managedUser.getMfaInfo();
        if (currentMfaInfo != null) {
            MfaInfo updatedMfaInfo = new MfaInfo(false, currentMfaInfo.isMfaEnforced(), null, null);
            managedUser.updateMfaInfo(updatedMfaInfo);
            // Persist user changes
            try {
//                userService.updateUserProfile(managedUser.getUserId(), null); // Use existing service method
                 userRepository.save(managedUser); // Or save directly
                log.info("MFA secret deleted and MFA disabled for user: {}", managedUser.getUsername());
            } catch (Exception e) {
                log.error("Failed to persist User entity after deleting MFA secret for user {}.", managedUser.getUsername(), e);
                throw new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "Failed to update user profile after deleting MFA secret", e);
            }
        }
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteAllMfaTokens() {
        log.warn("ADMIN ACTION: Deleting ALL MFA tokens from the repository!");
        long count = mfaTokenRepository.count();
        mfaTokenRepository.deleteAll();
        log.info("ADMIN ACTION: Deleted {} MFA tokens.", count);
        // Note: This leaves User entities potentially inconsistent. Requires separate action to update User.mfaInfo.
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteAllSecrets() {
        log.warn("ADMIN ACTION: Deleting ALL MFA secrets (by deleting all tokens)!");
        deleteAllMfaTokens(); // Deleting tokens effectively deletes secrets stored there
        log.warn("ADMIN ACTION: deleteAllSecrets executed. MFA tokens deleted. User entities may need manual update.");
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteAllMfaSettings() {
        log.warn("ADMIN ACTION: Deleting ALL MFA settings from the repository!");
        mfaSettingsRepository.deleteAll();
        log.info("ADMIN ACTION: Deleted MFA settings.");
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteAllMfaData() {
        log.warn("ADMIN ACTION: Deleting ALL MFA data (Tokens and Settings)!");
        deleteAllMfaTokens(); // Deletes tokens
        deleteAllMfaSettings(); // Deletes global settings
        log.warn("ADMIN ACTION: deleteAllMfaData executed. Tokens and settings deleted. User entities may need manual update.");
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteAllMfaDataForUser(User user) {
        // Changed log level from warn to info
        log.info("ADMIN ACTION: Deleting all MFA data for user: {}", user != null ? user.getUsername() : "null");
        if (user != null) {
            User managedUser = userService.getUserById(user.getUserId());
            disableMfaForUser(managedUser); // Handles token deletion and user entity update
        } else {
            throw new IllegalArgumentException("User cannot be null for deleting MFA data.");
        }
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteAllMfaDataForAllUsers() {
        log.warn("ADMIN ACTION: Deleting all MFA data for ALL users (Tokens)!");
        deleteAllMfaTokens();
        log.warn("ADMIN ACTION: deleteAllMfaDataForAllUsers executed. Tokens deleted. User entities may need manual update.");
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteAllMfaDataForAllUsersAndSecrets() {
        log.warn("ADMIN ACTION: Deleting all MFA data for ALL users (Tokens/Secrets)!");
        deleteAllSecrets(); // Handles token deletion and warns about User entity state
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true)
    public void deleteAllMfaDataForAllUsersAndSecretsAndTokens() {
        log.warn("ADMIN ACTION: Deleting all MFA data for ALL users (Tokens/Secrets)!");
        deleteAllSecrets(); // Handles token deletion and warns about User entity state
    }

    @Override
    @Transactional
    @RequiresAuthentication
    @RequiresRole(value = {ROLE_ADMIN, ROLE_SUPER_ADMIN, ROLE_ROOT}, allowMultiple = true) // Kept ROLE_ADMIN, adjust if needed
    public void deleteAllMfaDataForAllUsersAndSecretsAndTokensAndSettings() {
        log.warn("ADMIN ACTION: Deleting all MFA data (Tokens/Secrets/Settings)!");
        deleteAllSecrets(); // Handles tokens/secrets
        deleteAllMfaSettings(); // Handles settings
        log.warn("ADMIN ACTION: deleteAllMfaDataForAllUsersAndSecretsAndTokensAndSettings executed. Tokens, secrets, settings deleted. User entities may need manual update.");
    }

    // --- End Administrative Methods ---


    @Override
    public boolean isRateLimitExceeded(User user) {
        if (user == null || user.getUsername() == null) {
            log.warn("isRateLimitExceeded called with null user or username.");
            return true; // Fail-safe: assume exceeded if user is invalid
        }
        String rateLimitKey = getRateLimitKey(user.getUsername());
        try {
            // tryAcquire returns true if allowed, false if denied (limit exceeded)
            boolean allowed = rateLimiter.tryAcquire(rateLimitKey);
            if (!allowed) {
                log.warn("MFA rate limit exceeded for user: {}", user.getUsername());
            }
            return !allowed; // Return true if limit IS exceeded (i.e., !allowed)
        } catch (Exception e) {
            // Handle exceptions from the rate limiter (e.g., Redis connection error)
            log.error("Error checking rate limit for user {}: {}. Assuming limit exceeded (fail-safe).",
                    user.getUsername(), e.getMessage(), e);
            return true; // Fail-safe: assume limit exceeded if the checker fails
        }
    }

    // Helper to generate a consistent rate limit key for MFA verification attempts
    private String getRateLimitKey(String username) {
        // Use a prefix to avoid collisions with other rate limits
        return "mfa_verify:" + username;
    }
}