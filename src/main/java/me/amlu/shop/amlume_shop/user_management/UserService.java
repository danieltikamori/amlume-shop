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

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.exceptions.RoleNotFoundException;
import me.amlu.shop.amlume_shop.exceptions.UserAlreadyExistsException;
import me.amlu.shop.amlume_shop.user_management.dto.UserProfileUpdateRequest;
import me.amlu.shop.amlume_shop.user_management.dto.UserRegistrationRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {

    /**
     * Loads user details by username for authentication purposes.
     * This method is typically used by Spring Security's authentication provider.
     *
     * @param username The username (e.g., email) of the user to load.
     * @return A {@link UserDetails} object containing the user's information.
     * @throws UsernameNotFoundException If the user with the given username is not found.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * UserDetails userDetails = userService.loadUserByUsername("user@example.com");
     * // Use userDetails for authentication or authorization checks
     * }</pre>
     *
     * <p><b>Important Note:</b></p>
     * This method is primarily for Spring Security integration. For retrieving
     * user profiles for display, consider using methods that return a more
     * specific DTO like {@link UserProfileUpdateRequest} or a custom {@code UserProfileDTO}.
     * Caching strategies for this method should consider the frequency of authentication
     * attempts and the sensitivity of user data.
     */
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    /**
     * Retrieves the currently authenticated user.
     * This method typically relies on Spring Security's {@code SecurityContextHolder}.
     *
     * @return The {@link User} object representing the currently authenticated user, or {@code null} if no user is authenticated.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * User currentUser = userService.getCurrentUser();
     * if (currentUser != null) {
     *     System.out.println("Current user: " + currentUser.getEmail());
     * } else {
     *     System.out.println("No user authenticated.");
     * }
     * }</pre>
     *
     * <p><b>Security Consideration:</b></p>
     * Ensure that the underlying implementation correctly retrieves the user from the security context
     * and handles cases where no user is authenticated gracefully.
     */
    @Nullable
    User getCurrentUser();

    /**
     * Retrieves the ID of the currently authenticated user.
     *
     * @return The ID of the currently authenticated user, or {@code null} if no user is authenticated.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * Long currentUserId = userService.getCurrentUserId();
     * if (currentUserId != null) {
     *     System.out.println("Current user ID: " + currentUserId);
     * } else {
     *     System.out.println("No user authenticated.");
     * }
     * }</pre>
     */
    @Nullable
    Long getCurrentUserId();

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param userId The ID of the user to retrieve.
     * @return The {@link User} object if found, otherwise {@code null}.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * User user = userService.getUserById(123L);
     * if (user != null) {
     *     System.out.println("Found user: " + user.getEmail());
     * } else {
     *     System.out.println("User not found.");
     * }
     * }</pre>
     */
    @Nullable
    User getUserById(Long userId);

    /**
     * Retrieves user details by user ID, typically for internal security checks or specific use cases.
     *
     * @param userId The ID of the user to retrieve details for.
     * @return A {@link UserDetails} object containing the user's information.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     *
     *                                   <p><b>Usage:</b></p>
     *                                   <pre>{@code
     *                                   try {
     *                                       UserDetails userDetails = userService.getUserDetails(456L);
     *                                       System.out.println("User details for ID 456: " + userDetails.getUsername());
     *                                   } catch (UsernameNotFoundException e) {
     *                                       System.err.println("User with ID 456 not found.");
     *                                   }
     *                                   }</pre>
     */
    UserDetails getUserDetails(Long userId);

    /**
     * Updates the profile information for a specific user.
     * This method should be transactional to ensure data consistency.
     *
     * @param userId The ID of the user whose profile is to be updated.
     * @param profileRequest A {@link UserProfileUpdateRequest} containing the new profile data.
     *                       This DTO should be validated using {@code @Valid}.
     * @return The updated {@link User} object.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     * @throws IllegalArgumentException If the provided profile data is invalid (e.g., email already exists).
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest();
     * updateRequest.setFirstName("Jane");
     * updateRequest.setLastName("Doe");
     * updateRequest.setEmail("jane.doe@example.com");
     *
     * try {
     *     User updatedUser = userService.updateUserProfile(123L, updateRequest);
     *     System.out.println("User profile updated for: " + updatedUser.getEmail());
     * } catch (UsernameNotFoundException e) {
     *     System.err.println("User not found for update.");
     * } catch (IllegalArgumentException e) {
     *     System.err.println("Invalid profile data: " + e.getMessage());
     * }
     * }</pre>
     */
    @Transactional
    User updateUserProfile(Long userId, @Valid UserProfileUpdateRequest profileRequest);

    /**
     * Deletes a user from the system.
     * This operation should be handled with care as it is irreversible.
     * It should be transactional to ensure all related data is cleaned up.
     *
     * @param userId The ID of the user to delete.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     * @throws IllegalStateException If the user cannot be deleted due to business rules (e.g., has active orders).
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * try {
     *     userService.deleteUser(789L);
     *     System.out.println("User with ID 789 deleted successfully.");
     * } catch (UsernameNotFoundException e) {
     *     System.err.println("User with ID 789 not found for deletion.");
     * } catch (IllegalStateException e) {
     *     System.err.println("Failed to delete user: " + e.getMessage());
     * }
     * }</pre>
     *
     * <p><b>Security Consideration:</b></p>
     * Ensure proper authorization checks are in place before allowing a user to be deleted.
     * Consider soft-deletion (marking as inactive) instead of hard-deletion for auditing or recovery purposes.
     */
    @Transactional
    void deleteUser(Long userId);

    /**
     * Checks if a user with the given email address already exists in the system.
     * This is useful during user registration to prevent duplicate accounts.
     *
     * @param email The email address to check.
     * @return {@code true} if a user with the email exists, {@code false} otherwise.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * if (userService.existsByEmail("newuser@example.com")) {
     *     System.out.println("Email already registered.");
     * } else {
     *     System.out.println("Email available for registration.");
     * }
     * }</pre>
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user with the given ID exists in the system.
     *
     * @param userId The user ID to check.
     * @return {@code true} if a user with the ID exists, {@code false} otherwise.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * if (userService.existsById(123L)) {
     *     System.out.println("User with ID 123 exists.");
     * } else {
     *     System.out.println("User with ID 123 does not exist.");
     * }
     * }</pre>
     */
    boolean existsById(Long userId);

    /**
     * Finds a user by their email address.
     *
     * @param username The email address of the user.
     * @return The {@link User} object if found, otherwise {@code null}.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * User user = userService.findUserByEmail("admin@example.com");
     * if (user != null) {
     *     System.out.println("Found user: " + user.getFirstName());
     * } else {
     *     System.out.println("User not found.");
     * }
     * }</pre>
     */
    @Nullable
    User findUserByEmail(String username);

    /**
     * Checks if a given user has a specific role.
     *
     * @param user The {@link User} object to check.
     * @param role The name of the role (e.g., "ADMIN", "CUSTOMER").
     * @return {@code true} if the user has the specified role, {@code false} otherwise.
     * @throws IllegalArgumentException If the user object is null.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * User currentUser = userService.getCurrentUser();
     * if (currentUser != null && userService.hasRole(currentUser, "ADMIN")) {
     *     System.out.println("Current user is an administrator.");
     * } else {
     *     System.out.println("Current user is not an administrator.");
     * }
     * }</pre>
     */
    boolean hasRole(User user, String role);

    /**
     * Updates the last login timestamp for a user.
     * This method should be called upon successful user authentication.
     *
     * @param userId The ID of the user whose last login time is to be updated.
     * @throws UsernameNotFoundException If the user with the given ID is not found.
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * // After successful authentication
     * userService.updateLastLoginTime(authenticatedUserId);
     * }</pre>
     *
     * <p><b>Performance Note:</b></p>
     * This operation should be quick and ideally part of the authentication flow.
     */
    @Transactional
    void updateLastLoginTime(Long userId);
}
