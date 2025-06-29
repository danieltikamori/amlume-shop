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

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.cache_management.service.CacheService;
import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
import me.amlu.shop.amlume_shop.exceptions.UserNotFoundException;
import me.amlu.shop.amlume_shop.user_management.dto.UserProfileUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Objects;

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
    private final WebClient authServerAdminWebClient;
    private final CacheService cacheService; // Keep if providing specific caching beyond annotations
    private final UserService self; // Use the interface


    /**
     * Constructor for dependency injection.
     *
     * @param userRepository  The repository for user data access.
     * @param passwordEncoder The encoder for hashing passwords.
     * @param cacheService    The service for custom caching operations.
     */
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, WebClient authServerAdminWebClient, CacheService cacheService, @Lazy UserService self) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authServerAdminWebClient = authServerAdminWebClient;
        this.cacheService = cacheService;
        this.self = self;
    }

    // --- Registration ---

    // Admin registration is delegated to authserver admin API

    // --- User Retrieval ---

    /**
     * Finds a user by their username string. Results are cached.
     *
     * @param userEmail The username string to search for.
     * @return The found User entity.
     * @throws UserNotFoundException If no user is found with the given username.
     */
    @Override
    // Consider caching UserDetails instead of the full User entity for security context.
    // However, since User implements UserDetails and uses embedded objects, caching
    // the User object directly is a common and often acceptable approach.
    // Ensure the User entity and all embedded objects are Serializable for cache providers like Redis.
    @Cacheable(value = USERS_CACHE, key = "#userEmail") // Cache name from Constants.java
    public User findUserByEmail(String userEmail) {
        // 1. Precondition Check: Ensure username (userEmail) is not null or empty.
        Assert.hasText(userEmail, "Username (userEmail) must not be empty");
        log.debug("Attempting to find user by username (userEmail): {}", userEmail);

        // 2. Repository Call: Find user by username within the AuthenticationInfo embedded object.
        // The repository method returns Optional<User>.
        User user = userRepository.findByContactInfoUserEmailEmail(userEmail)
                // 3. Handle Not Found: Throw a specific custom exception if the Optional is empty.
                .orElseThrow(() -> {
                    log.warn("User not found with username (userEmail): {}", userEmail);
                    return new UserNotFoundException("User not found with username (userEmail): " + userEmail);
                });

        log.debug("Successfully found user by username (userEmail): {}", userEmail);
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
     * Implementation of {@link UserDetailsService#loadUserByUsername(String)}.
     * Loads user-specific data by username for Spring Security authentication.
     * Delegates to {@link #findUserByEmail(String)} and converts the exception type.
     *
     * @param username the username identifying the user whose data is required.
     * @return a fully populated user record (never {@code null}).
     * @throws UsernameNotFoundException if the user could not be found or the user has no GrantedAuthority.
     */
    @Override
    // Action: Keep method signature as required by UserDetailsService
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // The 'username' parameter here is the identifier Spring Security uses.
        // With OAuth2/OIDC, this will typically be the 'sub' claim from the token,
        // or the username from form login (which is userEmail in authserver).
        // In authserver, UserDetails.getUsername() returns userEmail.
        // So, the 'username' parameter here will be the userEmail.

        log.debug("loadUserByUsername (from Spring Security): username={}", username);

        try {
            // Action: Call the method that finds by userEmail (which was findUserByEmail, now renamed)
            // Assuming findUserByEmail is renamed to findUserByEmail:
            return self.findUserByEmail(username); // Call the renamed method (findUserByEmail)
            // Or directly:
            // return userRepository.findByContactInfoUserEmailEmail(username)
            //         .orElseThrow(() -> new UserNotFoundException("User not found with userEmail: " + username));

        } catch (UserNotFoundException e) {
            // Convert custom exception to Spring Security's exception
            log.warn("User not found for loadUserByUsername (userEmail): {}", username);
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
    @Timed(value = "shopapp.userservice.currentuser", extraTags = {"method", "getCurrentUser"}, description = "Time taken to get current user")
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
            User currentUser = userRepository.findByAuthServerSubjectId(authServerSubjectId)
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


    /**
     * Retrieves the ID of the currently authenticated user.
     *
     * @return The ID of the authenticated user.
     * @throws UnauthorizedException If no user is authenticated or the user is anonymous.
     */
    @Override
    public Long getCurrentUserId() {
        // Delegates to getCurrentUser() which handles authentication checks
        // This method is fine as is, assuming getCurrentUser is correctly implemented
        return Objects.requireNonNull(this.getCurrentUser()).getUserId();
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
    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')") // REALLYNECESSARY? Check if it is necessary to be authenticated user
    @Timed(value = "shopapp.userservice.updateUserProfile", extraTags = {"method", "updateUserProfile"}, description = "Time taken to update user profile")
    public User updateUserProfile(Long userId, @Valid UserProfileUpdateRequest profileRequest) {
        Assert.notNull(userId, "User ID must not be null");
        Assert.notNull(profileRequest, "Profile request cannot be null");

        User userToUpdate = self.getUserById(userId); // Fetch the existing user (uses cache if available)

        // --- Map *only* profile-related fields from DTO to entity ---
        // Example: Assuming UserProfileUpdateRequest has relevant fields
        // Use the 'with...' methods assuming ContactInfo is immutable or provides them
        assert userToUpdate != null;
        ContactInfo updatedContactInfo = userToUpdate.getContactInfo(); // Get current embedded

        boolean changed = false;

        if (profileRequest.getGivenName() != null) {
            // Assuming ContactInfo has a withGivenName method
            ContactInfo newContactInfo = updatedContactInfo.withGivenName(profileRequest.getGivenName());
            if (!newContactInfo.equals(updatedContactInfo)) { // Check if change occurred
                updatedContactInfo = newContactInfo;
                changed = true;
            }
        }
        if (profileRequest.getSurname() != null) {
            // Assuming ContactInfo has a withSurname method
            ContactInfo newContactInfo = updatedContactInfo.withSurname(profileRequest.getSurname());
            if (!newContactInfo.equals(updatedContactInfo)) { // Check if change occurred
                updatedContactInfo = newContactInfo;
                changed = true;
            }
        }
        // Add other updatable profile fields (phone number, etc.) from UserProfileUpdateRequest
        // Example: if (profileRequest.getPhoneNumber() != null) {
        //    ContactInfo newContactInfo = updatedContactInfo.withPhoneNumber(profileRequest.getPhoneNumber());
        //    if (!newContactInfo.equals(updatedContactInfo)) {
        //         updatedContactInfo = newContactInfo;
        //         changed = true;
        //    }
        // }


        // Set the potentially new embedded instance back onto the user entity
        // Only update if ContactInfo actually changed
        if (changed) {
            userToUpdate.updateContactInfo(updatedContactInfo); // Assuming User has updateContactInfo method
        }


        // Updating MFA preference (if allowed in profile update)
        // Consider if this belongs here or in a dedicated security settings update method
        // MfaInfo updatedMfaInfo = userToUpdate.getMfaInfo().withMfaEnabled(profileRequest.isMfaEnabled());
        // userToUpdate.updateMfaInfo(updatedMfaInfo);

        // --- DO NOT update sensitive fields like userEmail, username, password, roles here ---
        // These should have dedicated methods/flows with appropriate security checks.
        // Email and password updates should happen via authserver's API.

        User savedUser = userRepository.save(userToUpdate);
        log.info("Updated profile for user ID: {}", userId);
        return savedUser;
    }

    // Add dedicated methods for other updates, e.g.:
    // public void changeUserPassword(Long userId, String oldPassword, String newPassword) { ... } // Should call authserver API
    // public void updateUserEmail(Long userId, String newEmail) { ... } // Should call authserver API and trigger verification flow
    // public void updateUserRoles(Long userId, Set<AppRole> newRoles) { ... } // Should call authserver admin API


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
        User userToDelete = self.getUserById(userId);
        // Assuming deleteById triggers the @SoftDelete mechanism via AuditingEntityListener/Hibernate interceptor
        // If not, need a custom repository method: userRepository.softDelete(userToDelete);
        assert userToDelete != null;
        userRepository.delete(userToDelete); // Use delete(entity) which works well with @SoftDelete
        log.info("Soft deleted user with ID: {}", userId);
    }

    // --- Existence Checks ---

    @Override
    public boolean existsByEmail(String email) {
        Assert.hasText(email, "Email must not be empty");
        return userRepository.existsByContactInfoUserEmailEmail(email);
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
     * Updates the last login timestamp for a user to the current time.
     * Tracks Amlume-shop access time, distinct from AuthServer
     * It should be called by the OidcUserService/OAuth2UserService after successful OAuth2 login.
     * Evicts relevant caches.
     * It uses a targeted repository update.
     *
     * @param userId The ID of the user.
     */
    @Transactional
    @CacheEvict(value = {USERS_CACHE, CURRENT_USER_CACHE}, key = "#userId") // Evict cache as status changed
    @Override
    public void updateLastLoginTime(Long userId) {
        Assert.notNull(userId, "User ID must not be null");
        // Use targeted repository update
        userRepository.updateLastLoginTime(userId, Instant.now());
        log.trace("Updated last login timestamp for local user ID: {}", userId);
    }
}
