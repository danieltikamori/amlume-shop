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
import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
import me.amlu.shop.amlume_shop.exceptions.UserAlreadyExistsException;
import me.amlu.shop.amlume_shop.exceptions.UserNotFoundException; // Custom exception
import me.amlu.shop.amlume_shop.model.AppRole;
import me.amlu.shop.amlume_shop.payload.UserDTO; // Assuming this exists for isValidUser
import me.amlu.shop.amlume_shop.payload.user.UserProfileUpdateRequest; // Example DTO for profile update
import me.amlu.shop.amlume_shop.payload.user.UserRegistrationRequest;
import me.amlu.shop.amlume_shop.service.CacheService; // Keep if custom cache logic is essential
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
// Import UsernameNotFoundException from Spring Security if used, or custom UserNotFoundException
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert; // For assertions

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static me.amlu.shop.amlume_shop.commons.Constants.*;

@Service
@Transactional // Apply transactionality to all public methods by default
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // Removed self-injection: private final UserServiceImpl userService;
    private final CacheService cacheService; // Keep if providing specific caching beyond annotations

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, CacheService cacheService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cacheService = cacheService;
    }

    // --- Registration ---

    @Override
    public User registerUser(@Valid UserRegistrationRequest request) throws RoleNotFoundException, UserAlreadyExistsException {
        Assert.notNull(request, "Registration request cannot be null");

        // Check if user already exists by username or email
        if (userRepository.existsByAuthenticationInfoUsername(String.valueOf(request.getUsername()))) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' already taken");
        }
        if (userRepository.existsByContactInfoUserEmailEmail(request.getUserEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.getUserEmail() + "' already registered");
        }

        // Create new user with hashed password and initial details
        User user = User.builder()
                .authenticationInfo(AuthenticationInfo.builder()
                        .username(request.getUsername())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .build())
                .contactInfo(ContactInfo.builder()
                        .userEmail(new UserEmail(request.getUserEmail()))
                        // Add other contact fields if available in request
                        .build())
                .mfaInfo(MfaInfo.builder()
                        .mfaEnabled(request.isMfaEnabled())
                        // Initialize other MFA fields if necessary
                        .build())
                .accountStatus(AccountStatus.builder()
                        .creationTime(Instant.now()) // Set creation time
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .enabled(true) // Or false if email verification is required first
                        .failedLoginAttempts(0)
                        .build())
                .deviceFingerprintingInfo(DeviceFingerprintingInfo.builder()
                        .deviceFingerprintingEnabled(false) // Default to disabled?
                        .build())
                // Initialize other embedded objects as needed
                .build();

        // Assign default roles (Consider making default role configurable)
        Set<UserRole> roles = new HashSet<>();
        // Assuming UserRole has a constructor or static factory for AppRole
        roles.add(new UserRole(AppRole.ROLE_CUSTOMER));
        user.setRoles(roles);

        // TODO: Implement email verification logic if required
        // String verificationToken = generateVerificationToken(request.getEmail());
        // sendVerificationEmail(request.getEmail(), verificationToken);
        // user.setEmailVerificationToken(verificationToken);
        // user.setAccountStatus(user.getAccountStatus().withEnabled(false)); // Example

        User savedUser = userRepository.save(user);
        log.info("Registered new user: {} (ID: {})", savedUser.getUsername(), savedUser.getUserId());
        return savedUser;
    }

    // --- User Retrieval ---

    @Override
    // Consider caching UserDetails instead of the full User entity for security context
    // @Cacheable(value = USER_CACHE, key = "#username") // Example caching
    public User findUserByUsername(String username) {
        Assert.hasText(username, "Username must not be empty");
        return userRepository.findByAuthenticationInfoUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    @Override
    // @Cacheable(value = USER_CACHE, key = "#userId") // Example caching
    public User getUserById(Long userId) {
        Assert.notNull(userId, "User ID must not be null");
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    @Override
    // @Cacheable(value = USER_CACHE, key = "'email:' + #email") // Example caching key
    public User getUserByEmail(String email) {
        Assert.hasText(email, "Email must not be empty");
        // Assuming repository method returns Optional now for consistency
        return userRepository.findByContactInfoUserEmailEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) {
        Assert.hasText(usernameOrEmail, "Username or email must not be empty");
        // Assuming repository method handles searching both fields
        return userRepository.findByAuthenticationInfoUsernameOrContactInfoUserEmailEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with username or email: " + usernameOrEmail));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Delegate to findUserByUsername which throws custom exception
        // Spring Security expects UsernameNotFoundException
        try {
            return findUserByUsername(username);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        }
    }

    // --- Current User ---

    @Override
    // Caching the current user can be tricky due to keying and potential staleness.
    // Often better to fetch when needed or cache specific, less volatile data.
    // @Cacheable(value = CURRENT_USER_CACHE, key = "authentication.name") // Key needs context
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("No authenticated user found or user is anonymous");
        }

        String currentUsername = authentication.getName();
        // Fetch user by username from the authentication context
        return findUserByUsername(currentUsername); // Reuse existing method
    }

    @Override
    public Long getCurrentUserId() {
        return this.getCurrentUser().getUserId();
    }

    // --- User Details for Caching (Example using CacheService) ---

    @Override
    public UserDetails getUserDetails(Long userId) { // Changed parameter to Long
        Assert.notNull(userId, "User ID must not be null");
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        // Assuming CacheService handles caching UserDetails or similar lightweight object
        return cacheService.getOrCache(
                USER_CACHE,
                cacheKey,
                () -> userRepository.findById(userId) // Fetch the full User entity
                        .map(user -> (UserDetails) user) // Cast to UserDetails
                        .orElseThrow(() -> new UserNotFoundException("User not found for UserDetails lookup with ID: " + userId))
        );
    }

    // --- User Updates (Targeted and Safer) ---

    @CacheEvict(value = {USER_CACHE, CURRENT_USER_CACHE}, key = "#result.userId")
    @Override
    public User updateUserProfile(Long userId, @Valid UserProfileUpdateRequest profileRequest) {
        Assert.notNull(userId, "User ID must not be null");
        Assert.notNull(profileRequest, "Profile request cannot be null");

        User userToUpdate = getUserById(userId); // Fetch the existing user

        // --- Map *only* profile-related fields from DTO to entity ---
        // Example: Assuming UserProfileUpdateRequest has relevant fields
        ContactInfo updatedContactInfo = userToUpdate.getContactInfo(); // Get current embedded
        if (profileRequest.getFirstName() != null) {
            updatedContactInfo = updatedContactInfo.withFirstName(profileRequest.getFirstName());
        }
        if (profileRequest.getLastName() != null) {
            updatedContactInfo = updatedContactInfo.withLastName(profileRequest.getLastName());
        }
        // Add other updatable profile fields (phone number, etc.)
        userToUpdate.setContactInfo(updatedContactInfo); // Set the potentially new embedded instance

        // Updating MFA preference (if allowed in profile update)
        // MfaInfo updatedMfaInfo = userToUpdate.getMfaInfo().withMfaEnabled(profileRequest.isMfaEnabled());
        // userToUpdate.setMfaInfo(updatedMfaInfo);

        // --- DO NOT update sensitive fields like email, username, password, roles here ---
        // These should have dedicated methods/flows.

        User savedUser = userRepository.save(userToUpdate);
        log.info("Updated profile for user ID: {}", userId);
        return savedUser;
    }

    // Add dedicated methods for other updates, e.g.:
    // public void changeUserPassword(Long userId, String oldPassword, String newPassword) { ... }
    // public void updateUserEmail(Long userId, String newEmail) { ... } // Requires verification flow
    // public void updateUserRoles(Long userId, Set<AppRole> newRoles) { ... }

    // --- Deletion (Soft Delete) ---

    @Override
    @CacheEvict(value = {USER_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict relevant caches
    public void deleteUser(Long userId) {
        Assert.notNull(userId, "User ID must not be null");
        // Fetch user to ensure it exists before attempting delete
        User userToDelete = this.getUserById(userId);
        // Assuming deleteById triggers the @SoftDelete mechanism via AuditingEntityListener/Hibernate interceptor
        // If not, need a custom repository method: userRepository.softDelete(userToDelete);
        userRepository.delete(userToDelete); // Use delete(entity) which works well with @SoftDelete
        log.info("Soft deleted user with ID: {}", userId);
    }

    // --- Existence Checks ---

    @Override
    public boolean existsByUsername(String username) {
        Assert.hasText(username, "Username must not be empty");
        return userRepository.existsByAuthenticationInfoUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        Assert.hasText(email, "Email must not be empty");
        return userRepository.existsByContactInfoUserEmailEmail(email);
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        Assert.hasText(username, "Username must not be empty");
        Assert.hasText(email, "Email must not be empty");
        return userRepository.existsByAuthenticationInfoUsername(username) || userRepository.existsByContactInfoUserEmailEmail(email);
    }

    @Override
    public boolean existsById(Long userId) {
        Assert.notNull(userId, "User ID must not be null");
        return userRepository.existsById(userId);
    }

    // --- Role Check ---

    @Override
    public boolean hasRole(User user, String roleName) {
        Assert.notNull(user, "User cannot be null");
        Assert.hasText(roleName, "Role name must not be empty");
        return user.getRoles().stream()
                .anyMatch(userRole -> userRole.getRoleName().name().equals(roleName));
    }

    // --- Account Status Management ---

    @Transactional
    @Override
    public void incrementFailedLogins(String username) {
        User user = this.findUserByUsername(username); // Fetch fresh user state
        int newAttemptCount = user.getAccountStatus().getFailedLoginAttempts() + 1;

        // Use targeted repository update
        userRepository.updateFailedLoginAttempts(user.getUserId(), newAttemptCount);
        log.debug("Incremented failed login attempts for user '{}' to {}", username, newAttemptCount);

        if (newAttemptCount >= MAX_FAILED_ATTEMPTS) {
            // Call internal method directly using 'this'
            this.lockUserAccount(user.getUserId()); // Pass ID for consistency
        }
    }

    @Transactional
    @CacheEvict(value = {USER_CACHE, CURRENT_USER_CACHE}, key = "#userId")
    @Override
    public void lockUserAccount(Long userId) {
        Instant lockTime = Instant.now();
        // Use targeted repository update
        userRepository.updateAccountLockStatus(userId, true, lockTime); // Assuming true means locked
        log.warn("Locked account for user ID: {} at {}", userId, lockTime);
    }

    @Transactional
    @CacheEvict(value = {USER_CACHE, CURRENT_USER_CACHE}, key = "#userId")
    @Override
    public boolean unlockAccountIfExpired(Long userId) {
        User user = this.getUserById(userId); // Fetch fresh state
        AccountStatus status = user.getAccountStatus();

        if (status.isAccountNonLocked()) {
            log.trace("Account for user ID {} is already unlocked.", userId);
            return false; // Already unlocked
        }

        Instant lockTime = status.getLockTime();
        if (lockTime != null) {
            long lockTimeMillis = lockTime.toEpochMilli();
            long currentTimeMillis = System.currentTimeMillis();

            if (currentTimeMillis - lockTimeMillis >= LOCK_TIME_DURATION) {
                // Use targeted repository update to unlock and clear lock time
                userRepository.updateAccountLockStatus(userId, false, null);
                // Also reset failed attempts upon successful unlock
                this.resetFailedLoginAttempts(userId);
                log.info("Unlocked account for user ID: {} due to lock expiration.", userId);
                return true;
            } else {
                log.trace("Lock duration not yet expired for user ID {}.", userId);
            }
        } else {
            log.warn("User ID {} is locked but has no lock time recorded.", userId);
            // Optionally unlock anyway or investigate inconsistency
        }
        return false;
    }

    @Transactional
    @CacheEvict(value = {USER_CACHE, CURRENT_USER_CACHE}, key = "#userId")
    @Override
    public void resetFailedLoginAttempts(Long userId) {
        // Use targeted repository update
        userRepository.updateFailedLoginAttempts(userId, 0);
        log.debug("Reset failed login attempts for user ID: {}", userId);
    }

    // Removed redundant resetFailedLogins

    @Transactional
    @CacheEvict(value = {USER_CACHE, CURRENT_USER_CACHE}, key = "#userId")
    @Override
    public void updateLastLoginTime(Long userId) {
        // Use targeted repository update
        userRepository.updateLastLoginTime(userId, Instant.now());
        log.trace("Updated last login time for user ID: {}", userId);
    }

    // --- Validation Helper (Use with caution) ---

//    // This is a very basic check. Consider if it's truly needed
//    // or if DTO validation covers requirements.
//    public boolean isValidUserDto(UserDTO user) {
//        return user != null && user.getUserId() != null && user.getUsername() != null && user.getUserEmail() != null;
//    }
}

