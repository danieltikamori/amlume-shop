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
import me.amlu.shop.amlume_shop.user_management.dto.UserProfileUpdateRequest;
import me.amlu.shop.amlume_shop.user_management.dto.UserRegistrationRequest;
import me.amlu.shop.amlume_shop.cache_management.service.CacheService; // Keep if custom cache logic is essential
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
// Import UsernameNotFoundException from Spring Security if used, or custom UserNotFoundException
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert; // For assertions

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static me.amlu.shop.amlume_shop.commons.Constants.*;

/**
 * Service implementation for managing users, including registration, retrieval, updates,
 * and integration with Spring Security's UserDetailsService.
 */
@Service
@Transactional // Apply transactionality to all public methods by default
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // Removed self-injection: private final UserServiceImpl userService;
    private final CacheService cacheService; // Keep if providing specific caching beyond annotations

    /**
     * Constructor for dependency injection.
     *
     * @param userRepository  The repository for user data access.
     * @param passwordEncoder The encoder for hashing passwords.
     * @param cacheService    The service for custom caching operations.
     */
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, CacheService cacheService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cacheService = cacheService;
    }

    // --- Registration ---


    @Override
    @Transactional // Ensure atomicity
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, allEntries = true) // Evict user caches on new admin registration
    public User registerAdminUser(@Valid UserRegistrationRequest request) throws UserAlreadyExistsException, RoleNotFoundException {
        Assert.notNull(request, "Registration request cannot be null");

        String usernameString = request.getUsername(); // Gets the String value

        // Check if user already exists by username string or email
        if (userRepository.existsByAuthenticationInfoUsername_Username(usernameString)) {
            throw new UserAlreadyExistsException("Username '" + usernameString + "' already taken");
        }
        if (userRepository.existsByContactInfoUserEmailEmail(request.userEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.userEmail() + "' already registered");
        }

        // Hash password
        UserPassword encodedPassword = new UserPassword(passwordEncoder.encode(request.password().getPassword()));

        // Create a new user entity
        User user = User.builder()
                .authenticationInfo(AuthenticationInfo.builder()
                        .username(request.username()) // Pass the Username VO
                        .password(encodedPassword)
                        .build())
                .contactInfo(ContactInfo.builder()
                        .userEmail(new UserEmail(request.userEmail()))
                        // Add other contact fields if available in request (e.g., first/last name if added to DTO)
                        .build())
                .accountStatus(AccountStatus.builder()
                        .creationTime(Instant.now())
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .enabled(true) // Admins are typically enabled immediately
                        .failedLoginAttempts(0)
                        .build())
                .deviceFingerprintingInfo(DeviceFingerprintingInfo.builder()
                        .deviceFingerprintingEnabled(false) // Default for admin? Or configurable?
                        .build())
                .build();

        // --- Assign ADMIN Roles ---
        Set<UserRole> roles = new HashSet<>();
        // Assign core admin role
        roles.add(new UserRole(AppRole.ROLE_ADMIN));
        // Optionally assign base user role as well (hierarchy might cover this, but explicit is clearer)
        roles.add(new UserRole(AppRole.ROLE_USER));
        // Add other roles like ROLE_MANAGER if admins should inherit those permissions directly
        // roles.add(new UserRole(AppRole.ROLE_MANAGER));
        user.createRoleSet(roles);
        // --- End Role Assignment ---


        // TODO: Implement email verification logic if required for admins? Usually not.

        User savedUser = userRepository.save(user);
        log.info("Registered new ADMIN user: {} (ID: {})", savedUser.getUsername(), savedUser.getUserId());
        return savedUser;
    }

    /**
     * Registers a new user based on the provided request details.
     *
     * @param request The user registration request DTO.
     * @return The newly created and saved User entity.
     * @throws RoleNotFoundException      If the default role cannot be found (should not happen with hardcoded default).
     * @throws UserAlreadyExistsException If the username or email is already taken.
     */
    @Override
    public User registerUser(@Valid UserRegistrationRequest request) throws RoleNotFoundException, UserAlreadyExistsException {
        Assert.notNull(request, "Registration request cannot be null");

        // Use the specific username string getter from the request record
        String usernameString = request.getUsername(); // Gets the String value

        // Check if user already exists by username string or email
        // Use the updated repository method name
        if (userRepository.existsByAuthenticationInfoUsername_Username(usernameString)) {
            throw new UserAlreadyExistsException("Username '" + usernameString + "' already taken");
        }
        if (userRepository.existsByContactInfoUserEmailEmail(request.userEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.userEmail() + "' already registered");
        }

        // Hash password
        UserPassword encodedPassword = new UserPassword(passwordEncoder.encode(request.password().getPassword()));

        // Create a new user with hashed password and initial details
        // Pass the Username VO from the request to the builder
        User user = User.builder()
                .authenticationInfo(AuthenticationInfo.builder()
                        .username(request.username()) // Pass the Username VO
                        .password(encodedPassword)
                        .build())
                .contactInfo(ContactInfo.builder()
                        .userEmail(new UserEmail(request.userEmail()))
                        // Add other contact fields if available in request
                        .build())
                .accountStatus(AccountStatus.builder()
                        .creationTime(Instant.now()) // Set creation timestamp
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
        user.createRoleSet(roles);

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

    /**
     * Finds a user by their username string. Results are cached.
     *
     * @param username The username string to search for.
     * @return The found User entity.
     * @throws UserNotFoundException If no user is found with the given username.
     */
    @Override
    // Consider caching UserDetails instead of the full User entity for security context.
    // However, since User implements UserDetails and uses embedded objects, caching
    // the User object directly is a common and often acceptable approach.
    // Ensure the User entity and all embedded objects are Serializable for cache providers like Redis.
    @Cacheable(value = USERS_CACHE, key = "#username") // Cache name from Constants.java
    public User findUserByUsername(String username) {
        // 1. Precondition Check: Ensure username is not null or empty.
        Assert.hasText(username, "Username must not be empty");
        log.debug("Attempting to find user by username: {}", username); // Added debug log

        // 2. Repository Call: Find user by username within the AuthenticationInfo embedded object.
        // The repository method returns Optional<User>.
        User user = userRepository.findByAuthenticationInfoUsername_Username(username)
                // 3. Handle Not Found: Throw a specific custom exception if the Optional is empty.
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username); // Log warning on failure
                    return new UserNotFoundException("User not found with username: " + username);
                });

        // 4. Log Success and Return: Log if found (optional, could be verbose) and return the user.
        log.debug("Successfully found user by username: {}", username);
        return user;
    }

    /**
     * Retrieves a user by their unique ID. Results are cached.
     *
     * @param userId The ID of the user to retrieve.
     * @return The found User entity.
     * @throws UserNotFoundException If no user is found with the given ID.
     */
    @Override
    @Cacheable(value = USERS_CACHE, key = "#userId")
    public User getUserById(Long userId) {
        Assert.notNull(userId, "User ID must not be null");
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email The email address to search for.
     * @return The found User entity.
     * @throws UserNotFoundException If no user is found with the given email.
     */
    @Override
//     @Cacheable(value = USERS_CACHE, key = "'email:' + #email") // Consider caching if frequently used
    public User getUserByEmail(String email) {
        Assert.hasText(email, "Email must not be empty");
        // Assuming repository method returns Optional now for consistency
        return userRepository.findByContactInfoUserEmailEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    /**
     * Retrieves a user by either their username string or email address.
     *
     * @param usernameOrEmail The username string or email address to search for.
     * @return The found User entity.
     * @throws UserNotFoundException If no user is found with the given identifier.
     */
    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) {
        Assert.hasText(usernameOrEmail, "Username or email must not be empty");
        // Assuming repository method handles searching both fields
        return userRepository.findByAuthenticationInfoUsername_UsernameOrContactInfoUserEmailEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with username or email: " + usernameOrEmail));
    }

    /**
     * Implementation of {@link UserDetailsService#loadUserByUsername(String)}.
     * Loads user-specific data by username for Spring Security authentication.
     * Delegates to {@link #findUserByUsername(String)} and converts the exception type.
     *
     * @param username the username identifying the user whose data is required.
     * @return a fully populated user record (never {@code null}).
     * @throws UsernameNotFoundException if the user could not be found or the user has no GrantedAuthority.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Delegate to findUserByUsername which throws custom exception
        // Spring Security expects UsernameNotFoundException
        try {
            // findUserByUsername is already cached
            return findUserByUsername(username);
        } catch (UserNotFoundException e) {
            // Convert custom exception to Spring Security's exception
            throw new UsernameNotFoundException(e.getMessage(), e);
        }
    }

    // --- Current User ---

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
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("No authenticated user found or user is anonymous");
        }

        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            String userEmail = jwt.getSubject(); // 'sub' claim is the Keycloak User ID
            // String username = jwt.getClaimAsString("preferred_username"); // Or get username

            // Option 1: If you only need the ID/username from token
//             return createTransientUserFromJwt(jwt); // A helper to build a temporary User object

            // Option 2: If you need the full local User entity linked to Keycloak
//            return userRepository.findBykeycloakId(keycloakUserId) // Need this method in repo
//                    .orElseThrow(() -> new UserNotFoundException("Local user not found for Keycloak ID: " + keycloakUserId));\
            // Spring Authorization Server implementation

            userRepository.findByContactInfoUserEmailEmail(userEmail);
            return userRepository.findByContactInfoUserEmailEmail(userEmail)
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));
        } else {
            // Should not happen with JWT config, but handle defensively
            log.error("Unexpected principal type: {}", authentication.getPrincipal().getClass());
            throw new UnauthorizedException("Unexpected authentication principal type");
        }
    }


    /**
     * Retrieves the ID of the currently authenticated user.
     *
     * @return The ID of the authenticated user.
     * @throws UnauthorizedException If no user is authenticated or the user is anonymous.
     */
    @Override
    public Long getCurrentUserId() {
        // Delegates to getCurrentUser() which handles authentication checks
        return this.getCurrentUser().getUserId();
    }

    // --- User Details for Caching (Example using CacheService) ---

    /**
     * Retrieves UserDetails for a given user ID, utilizing the custom CacheService.
     *
     * @param userId The ID of the user.
     * @return The UserDetails object (which is the User entity itself).
     * @throws UserNotFoundException If the user is not found.
     */
    @Override
    public UserDetails getUserDetails(Long userId) { // Changed parameter to Long
        Assert.notNull(userId, "User ID must not be null");
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        // Assuming CacheService handles caching UserDetails or similar lightweight object
        return cacheService.getOrCache(
                USERS_CACHE,
                cacheKey,
                () -> userRepository.findById(userId) // Fetch the full User entity
                        .map(user -> (UserDetails) user) // Cast to UserDetails (User implements it)
                        .orElseThrow(() -> new UserNotFoundException("User not found for UserDetails lookup with ID: " + userId))
        );
    }

    // --- User Updates (Targeted and Safer) ---

    /**
     * Updates non-sensitive profile information for a user.
     * Evicts relevant caches upon successful update.
     *
     * @param userId         The ID of the user to update.
     * @param profileRequest The DTO containing the profile updates.
     * @return The updated User entity.
     * @throws UserNotFoundException If the user with the given ID is not found.
     */
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#result.userId") // Evict based on updated user's ID
    @Override
    public User updateUserProfile(Long userId, @Valid UserProfileUpdateRequest profileRequest) {
        Assert.notNull(userId, "User ID must not be null");
        Assert.notNull(profileRequest, "Profile request cannot be null");

        User userToUpdate = getUserById(userId); // Fetch the existing user (uses cache if available)

        // --- Map *only* profile-related fields from DTO to entity ---
        // Example: Assuming UserProfileUpdateRequest has relevant fields
        // Use the 'with...' methods assuming ContactInfo is immutable or provides them
        ContactInfo updatedContactInfo = userToUpdate.getContactInfo(); // Get current embedded
        if (profileRequest.getFirstName() != null) {
            updatedContactInfo = updatedContactInfo.withFirstName(profileRequest.getFirstName());
        }
        if (profileRequest.getLastName() != null) {
            updatedContactInfo = updatedContactInfo.withLastName(profileRequest.getLastName());
        }
        // Add other updatable profile fields (phone number, etc.)
        // Example: if (profileRequest.getPhoneNumber() != null) { updatedContactInfo = updatedContactInfo.withPhoneNumber(profileRequest.getPhoneNumber()); }

        // Set the potentially new embedded instance back onto the user entity
        userToUpdate.updateContactInfo(updatedContactInfo);

        // Updating MFA preference (if allowed in profile update)
        // Consider if this belongs here or in a dedicated security settings update method
        // MfaInfo updatedMfaInfo = userToUpdate.getMfaInfo().withMfaEnabled(profileRequest.isMfaEnabled());
        // userToUpdate.updateMfaInfo(updatedMfaInfo);

        // --- DO NOT update sensitive fields like email, username, password, roles here ---
        // These should have dedicated methods/flows with appropriate security checks.

        User savedUser = userRepository.save(userToUpdate);
        log.info("Updated profile for user ID: {}", userId);
        return savedUser;
    }

    // Add dedicated methods for other updates, e.g.:
    // public void changeUserPassword(Long userId, String oldPassword, String newPassword) { ... }
    // public void updateUserEmail(Long userId, String newEmail) { ... } // Requires verification flow
    // public void updateUserRoles(Long userId, Set<AppRole> newRoles) { ... }

    // --- Deletion (Soft Delete) ---

    /**
     * Soft deletes a user by marking them as deleted (assuming BaseEntity handles this).
     * Evicts relevant caches upon successful deletion.
     *
     * @param userId The ID of the user to delete.
     * @throws UserNotFoundException If the user with the given ID is not found.
     */
    @Override
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict relevant caches by user ID
    public void deleteUser(Long userId) {
        Assert.notNull(userId, "User ID must not be null");
        // Fetch user to ensure it exists before attempting delete (uses cache if available)
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
        return userRepository.existsByAuthenticationInfoUsername_Username(username);
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
        // Efficient check using OR condition
        return userRepository.existsByAuthenticationInfoUsername_Username(username) || userRepository.existsByContactInfoUserEmailEmail(email);
    }

    @Override
    public boolean existsById(Long userId) {
        Assert.notNull(userId, "User ID must not be null");
        return userRepository.existsById(userId);
    }

    // --- Role Check ---

    /**
     * Checks if a user has a specific role.
     *
     * @param user     The User entity.
     * @param roleName The name of the role (e.g., "ROLE_ADMIN").
     * @return true if the user has the role, false otherwise.
     */
    @Override
    public boolean hasRole(User user, String roleName) {
        Assert.notNull(user, "User cannot be null");
        Assert.hasText(roleName, "Role name must not be empty");
        // Ensure roles collection is not null before streaming
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(userRole -> {
                    // Ensure the roleName enum within UserRole is not null
                    assert userRole.getRoleName() != null : "UserRole contains a null roleName enum";
                    // Compare the string representation of the enum
                    return userRole.getRoleName().name().equals(roleName);
                });
    }

    // --- Account Status Management ---

    /**
     * Increments the failed login attempt count for a user. Locks the account if the maximum attempts are reached.
     * Uses a targeted repository update for efficiency.
     *
     * @param username The username of the user.
     * @throws UserNotFoundException If the user is not found.
     */
    @Transactional // Ensures atomicity of read, increment, check, and potential lock
    @Override
    // No cache eviction here, as only the count changes. Lock operation handles eviction.
    public void incrementFailedLogins(String username) {
        // Fetch fresh user state within the transaction to get the current attempt count
        // Note: findUserByUsername might hit cache, but subsequent DB update will be correct.
        // Consider if fetching directly from DB is needed for absolute guarantee on count before update.
        // For now, assuming cache consistency or accepting minor race condition risk.
        User user = this.findUserByUsername(username);
        int newAttemptCount = user.getAccountStatus().getFailedLoginAttempts() + 1;

        // Use targeted repository update for efficiency
        userRepository.updateFailedLoginAttempts(user.getUserId(), newAttemptCount);
        log.debug("Incremented failed login attempts for user '{}' to {}", username, newAttemptCount);

        // Check if lock threshold is reached
        if (newAttemptCount >= MAX_FAILED_ATTEMPTS) {
            // Call internal method directly using 'this' (will participate in the current transaction)
            this.lockUserAccount(user.getUserId()); // Pass ID for consistency
        }
    }

    /**
     * Locks a user account by setting the lock status and timestamp.
     * Evicts relevant caches. Uses a targeted repository update.
     *
     * @param userId The ID of the user to lock.
     */
    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict user details cache
    @Override
    public void lockUserAccount(Long userId) {
        Instant lockTime = Instant.now();
        // Use targeted repository update
        userRepository.updateAccountLockStatus(userId, false, lockTime); // false means locked
        log.warn("Locked account for user ID: {} at {}", userId, lockTime);
    }

    /**
     * Unlocks a user account if the lock duration has expired.
     * Resets failed login attempts upon successful unlock.
     * Evicts relevant caches. Uses targeted repository updates.
     *
     * @param userId The ID of the user to check and potentially unlock.
     * @return true if the account was unlocked, false otherwise.
     * @throws UserNotFoundException If the user is not found.
     */
    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict on potential unlock
    @Override
    public boolean unlockAccountIfExpired(Long userId) {
        User user = this.getUserById(userId); // Fetch fresh state (uses cache if available)
        AccountStatus status = user.getAccountStatus();

        // Check if already unlocked (accountNonLocked is true)
        if (status.isAccountNonLocked()) {
            log.trace("Account for user ID {} is already unlocked.", userId);
            return false; // Already unlocked, nothing to do
        }

        // Account is locked, check lock timestamp
        Instant lockTime = status.getLockTime();
        if (lockTime != null) {
            long lockTimeMillis = lockTime.toEpochMilli();
            long currentTimeMillis = System.currentTimeMillis(); // Use System.currentTimeMillis for efficiency

            // Check if lock duration has passed
            if (currentTimeMillis - lockTimeMillis >= LOCK_TIME_DURATION) {
                // Use targeted repository update to unlock (set accountNonLocked to true) and clear lock timestamp
                userRepository.updateAccountLockStatus(userId, true, null);
                // Also reset failed attempts upon successful unlock
                this.resetFailedLoginAttempts(userId); // Call the existing method (already transactional and evicts cache)
                log.info("Unlocked account for user ID: {} due to lock expiration.", userId);
                return true; // Account was unlocked
            } else {
                log.trace("Lock duration not yet expired for user ID {}.", userId);
            }
        } else {
            // Handle inconsistency: account is locked but has no lock timestamp
            log.warn("User ID {} is locked but has no lock timestamp recorded. Unlocking as a safety measure.", userId);
            // For safety, let's unlock if lockTime is null, but the account is locked
            userRepository.updateAccountLockStatus(userId, true, null);
            this.resetFailedLoginAttempts(userId);
            log.info("Unlocked account for user ID: {} due to missing lock timestamp.", userId);
            return true; // Account was unlocked (due to inconsistency handling)
        }
        return false; // Account remains locked (duration not expired)
    }

    /**
     * Resets the failed login attempt count for a user to zero.
     * Evicts relevant caches. Uses a targeted repository update.
     *
     * @param userId The ID of the user.
     */
    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict cache as status changed
    @Override
    public void resetFailedLoginAttempts(Long userId) {
        // Use targeted repository update
        userRepository.updateFailedLoginAttempts(userId, 0);
        log.debug("Reset failed login attempts for user ID: {}", userId);
    }

    // Removed redundant resetFailedLogins (was identical to resetFailedLoginAttempts)

    /**
     * Updates the last login timestamp for a user to the current time.
     * Evicts relevant caches. Uses a targeted repository update.
     *
     * @param userId The ID of the user.
     */
    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict cache as status changed
    @Override
    public void updateLastLoginTime(Long userId) {
        // Use targeted repository update
        userRepository.updateLastLoginTime(userId, Instant.now());
        log.trace("Updated last login timestamp for user ID: {}", userId);
    }

    // --- Validation Helper (Use with caution) ---

//    // This is a very basic check. Consider if it's truly necessary
//    // or if DTO validation covers requirements.
//    public boolean isValidUserDto(UserDTO user) {
//        return user != null && user.getUserId() != null && user.username() != null && user.userEmail() != null;
//    }
}