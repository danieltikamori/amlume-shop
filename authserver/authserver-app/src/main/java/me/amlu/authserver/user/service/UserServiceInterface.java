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

import jakarta.validation.Valid;
import io.micrometer.core.annotation.Timed;
import me.amlu.authserver.exceptions.ResourceNotFoundException;
import me.amlu.authserver.security.dto.DeviceRegistrationInfo;
import me.amlu.authserver.user.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public interface UserServiceInterface {

    /**
     * <p><b>Purpose:</b> To record and manage failed login attempts for a user.</p>
     * <p>This method is crucial for implementing security measures like account lockout policies
     * to prevent brute-force attacks.</p>
     * <p>
     * Handles failed login attempts for a given username.
     * This method is typically called by an authentication failure handler.
     *
     * @param username The username for which the login attempt failed.
     */
    void handleFailedLogin(String username);


    /**
     * <p><b>Purpose:</b> To process a successful user login, including security and device management updates.</p>
     * <p>This method performs several critical actions upon a successful login:</p>
     * <ul>
     *     <li>Resets any failed login attempt counters for the user.</li>
     *     <li>Unlocks the user's account if it was previously locked due to too many failed attempts.</li>
     *     <li>Updates or registers the device fingerprint associated with the login session. This helps in
     *         identifying known devices and can be used for risk-based authentication.</li>
     * </ul>
     *
     * Handles successful login attempts by resetting failed login counters,
     * unlocking accounts, and updating the device fingerprint.
     *
     * @param usernameEmail The username or email used in the successful login attempt. This parameter
     *                      is used to identify the user account.
     * @param deviceInfo    The device registration information, typically containing details like
     *                      device fingerprint, IP address, and user agent. This information is used
     *                      to manage trusted devices and detect suspicious login patterns.
     * @throws IllegalArgumentException if {@code usernameEmail} is null or empty, or if {@code deviceInfo} is null.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * // In an authentication success handler:
     * String username = authentication.getName();
     * DeviceRegistrationInfo device = new DeviceRegistrationInfo("fingerprint123", "192.168.1.1", "Mozilla/5.0...");
     * userService.handleSuccessfulLogin(username, device);
     * }</pre>
     */
    void handleSuccessfulLogin(String usernameEmail, @Valid DeviceRegistrationInfo deviceInfo);

    /**
     * <p><b>Purpose:</b> To create a new user account in the system.</p>
     * <p>This method is responsible for registering a new user with all necessary profile details,
     * including sensitive information like password and recovery email. It also incorporates
     * security measures such as CAPTCHA verification and IP address logging for audit purposes.</p>
     *
     * @param givenName       The user's given name (first name).
     * @param middleName      The user's middle name (can be null or empty).
     * @param surname         The user's surname (last name).
     * @param nickname        A chosen nickname for the user (can be null or empty).
     * @param email           The user's primary email address, which often serves as their username. Must be unique.
     * @param rawPassword     The user's chosen raw (unhashed) password. This will be securely hashed before storage.
     * @param mobileNumber    The user's mobile phone number (can be null or empty).
     * @param defaultRegion   The user's default geographical region, used for localization or service preferences.
     * @param recoveryEmailRaw The user's recovery email address (can be null or empty). This should be different from the primary email.
     * @param captchaResponse The response from a CAPTCHA challenge, used to verify that the request is not from a bot.
     * @param ipAddress       The IP address from which the user registration request originated, for auditing and security.
     * @return The newly created {@link User} object, representing the persisted user account.
     * @throws IllegalArgumentException if required fields like email or rawPassword are null or invalid,
     *                                  or if CAPTCHA verification fails.
     * @throws IllegalStateException    if a user with the given email already exists.
     *
     * <p><b>Important Notes:</b></p>
     * <ul>
     *     <li>The {@code rawPassword} is never stored directly; it's hashed using a strong,
     *         industry-standard algorithm (e.g., BCrypt) before persistence.</li>
     *     <li>The {@code captchaResponse} should be validated against a CAPTCHA service (e.g., reCAPTCHA)
     *         to prevent automated registrations.</li>
     *     <li>This method is {@code @Transactional}, ensuring atomicity of the user creation process.</li>
     *     <li>{@code @Timed} annotation is used for performance monitoring.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * User newUser = userService.createUser(
     *     "John", "D.", "Doe", "Johnny", "john.doe@example.com", "SecureP@ssw0rd!",
     *     "+15551234567", "US", "john.recovery@example.com", "captcha_token_from_frontend", "192.0.2.1"
     * );
     * System.out.println("User created with ID: " + newUser.getId());
     * }</pre>
     */
    @Transactional
    @Timed(value = "authserver.usermanager.create", description = "Time taken to create user")
    User createUser(String givenName, String middleName, String surname, String nickname,
                    String email, String rawPassword, String mobileNumber,
                    String defaultRegion, String recoveryEmailRaw, String captchaResponse, String ipAddress);

    /**
     * <p><b>Purpose:</b> To update the profile information of an existing user.</p>
     * <p>This method allows users or administrators to modify non-sensitive profile details
     * such as names, nickname, mobile number, and recovery email. It ensures that only
     * authorized changes are made to a user's profile.</p>
     *
     * @param userId           The unique identifier of the user whose profile is to be updated.
     * @param newGivenName     The new given name for the user (can be null if not changing).
     * @param newMiddleName    The new middle name for the user (can be null if not changing).
     * @param newSurname       The new surname for the user (can be null if not changing).
     * @param newNickname      The new nickname for the user (can be null if not changing).
     * @param newMobileNumber  The new mobile phone number for the user (can be null if not changing).
     * @param defaultRegion    The new default region for the user (can be null if not changing).
     * @param newRecoveryEmail The new recovery email address for the user (can be null if not changing).
     * @return The updated {@link User} object after persistence.
     * @throws IllegalArgumentException if {@code userId} is null or invalid, or if any provided new value is invalid.
     * @throws ResourceNotFoundException if no user with the given {@code userId} is found.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * User updatedUser = userService.updateUserProfile(
     *     123L, "Jane", null, "Doe", "Janie", "+15559876543", "CA", "jane.recovery@example.com"
     * );
     * System.out.println("User " + updatedUser.getNickname() + " profile updated.");
     * }</pre>
     */
    User updateUserProfile(Long userId, String newGivenName, String newMiddleName, String newSurname, String newNickname, String newMobileNumber, String defaultRegion, String newRecoveryEmail);

    /**
     * <p><b>Purpose:</b> To allow a user to change their own password.</p>
     * <p>This method requires the user to provide their current (old) password for verification
     * before setting a new one. This is a standard security practice to ensure that only
     * the legitimate user can change their password.</p>
     *
     * @param userId         The unique identifier of the user changing their password.
     * @param oldRawPassword The user's current raw (unhashed) password for verification.
     * @param newRawPassword The user's new raw (unhashed) password. This will be securely hashed before storage.
     * @throws IllegalArgumentException if {@code userId} is null, or if {@code oldRawPassword} or {@code newRawPassword} are invalid.
     * @throws SecurityException        if the {@code oldRawPassword} does not match the user's current password.
     * @throws ResourceNotFoundException if no user with the given {@code userId} is found.
     *
     * <p><b>Important Notes:</b></p>
     * <ul>
     *     <li>The {@code newRawPassword} is hashed before being stored.</li>
     *     <li>It's crucial to implement strong password policies (e.g., minimum length, complexity)
     *         when validating {@code newRawPassword}.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * try {
     *     userService.changeUserPassword(123L, "OldP@ssw0rd!", "NewSecureP@ssw0rd!");
     *     System.out.println("Password changed successfully.");
     * } catch (SecurityException e) {
     *     System.err.println("Failed to change password: " + e.getMessage());
     * }
     * }</pre>
     */
    void changeUserPassword(Long userId, String oldRawPassword, String newRawPassword);

    /**
     * <p><b>Purpose:</b> To allow an administrator to change a user's password without knowing the old password.</p>
     * <p>This method is intended for administrative use, typically for password resets or
     * in scenarios where an administrator needs to regain access to a user's account.
     * It requires specific administrative roles for authorization.</p>
     *
     * @param userId         The unique identifier of the user whose password is to be changed.
     * @param newRawPassword The new raw (unhashed) password for the user. This will be securely hashed before storage.
     * @throws IllegalArgumentException if {@code userId} is null or invalid, or if {@code newRawPassword} is invalid.
     * @throws ResourceNotFoundException if no user with the given {@code userId} is found.
     * @throws AccessDeniedException if the authenticated user does not have the required administrative roles.
     *
     * <p><b>Security Considerations:</b></p>
     * <ul>
     *     <li>This method is protected by {@code @PreAuthorize} to ensure only users with
     *         'ROLE_ADMIN', 'ROLE_SUPER_ADMIN', or 'ROLE_ROOT' can execute it.</li>
     *     <li>The {@code newRawPassword} is hashed before being stored.</li>
     *     <li>Proper logging of administrative actions should be in place when this method is called.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * // As an admin:
     * userService.adminChangeUserPassword(456L, "AdminSetNewP@ss!");
     * System.out.println("User 456 password changed by admin.");
     * }</pre>
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')") // Or based on who can change password
    @Timed(value = "authserver.usermanager.changepassword", description = "Time taken to admin change user password")
    void adminChangeUserPassword(Long userId, String newRawPassword);

    /**
     * <p><b>Purpose:</b> To allow an administrator to change a user's password using their username.</p>
     * <p>Similar to {@link #adminChangeUserPassword(Long, String)}, but identifies the user by their
     * username (typically email) instead of their ID. This is useful in administrative interfaces
     * where usernames are more commonly used for identification.</p>
     *
     * @param username       The username (e.g., email) of the user whose password is to be changed.
     * @param newRawPassword The new raw (unhashed) password for the user. This will be securely hashed before storage.
     * @throws IllegalArgumentException if {@code username} is null or empty, or if {@code newRawPassword} is invalid.
     * @throws ResourceNotFoundException if no user with the given {@code username} is found.
     * @throws AccessDeniedException if the authenticated user does not have the required administrative roles.
     *
     * <p><b>Security Considerations:</b></p>
     * <ul>
     *     <li>This method is protected by {@code @PreAuthorize} to ensure only users with
     *         'ROLE_ADMIN', 'ROLE_SUPER_ADMIN', or 'ROLE_ROOT' can execute it.</li>
     *     <li>The {@code newRawPassword} is hashed before being stored.</li>
     *     <li>Proper logging of administrative actions should be in place when this method is called.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * // As an admin:
     * userService.adminChangeUserPasswordByUsername("user@example.com", "AdminSetNewP@ss!");
     * System.out.println("User user@example.com password changed by admin.");
     * }</pre>
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    @Timed(value = "authserver.usermanager.changepassword", description = "Time taken to admin change user password")
    void adminChangeUserPasswordByUsername(String username, String newRawPassword);

    /**
     * <p><b>Purpose:</b> To manually unlock a user account that has been locked, typically due to too many failed login attempts.</p>
     * <p>This method provides an administrative override to restore access for a user whose account
     * has been temporarily or permanently locked by the system's security policies.</p>
     *
     * @param userId The unique identifier of the user account to be unlocked.
     * @throws IllegalArgumentException if {@code userId} is null or invalid.
     * @throws ResourceNotFoundException if no user with the given {@code userId} is found.
     * @throws AccessDeniedException if the authenticated user does not have the required administrative roles.
     *
     * <p><b>Security Considerations:</b></p>
     * <ul>
     *     <li>This method is protected by {@code @PreAuthorize} to ensure only users with
     *         'ROLE_ADMIN', 'ROLE_SUPER_ADMIN', or 'ROLE_ROOT' can execute it.</li>
     *     <li>Proper logging of administrative actions should be in place when this method is called.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * // As an admin:
     * userService.adminUnlockUser(789L);
     * System.out.println("User 789 account unlocked by admin.");
     * }</pre>
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    void adminUnlockUser(Long userId);

    /**
     * <p><b>Purpose:</b> To manually enable or disable a user account.</p>
     * <p>This method allows administrators to control the active status of a user account.
     * Disabling an account prevents the user from logging in, while enabling it restores access.
     * This is useful for managing user lifecycle, temporary suspensions, or permanent deactivation.</p>
     *
     * @param userId  The unique identifier of the user account to be enabled or disabled.
     * @param enabled A boolean value: {@code true} to enable the account, {@code false} to disable it.
     * @throws IllegalArgumentException if {@code userId} is null or invalid.
     * @throws ResourceNotFoundException if no user with the given {@code userId} is found.
     * @throws AccessDeniedException if the authenticated user does not have the required administrative roles.
     *
     * <p><b>Security Considerations:</b></p>
     * <ul>
     *     <li>This method is protected by {@code @PreAuthorize} to ensure only users with
     *         'ROLE_ADMIN', 'ROLE_SUPER_ADMIN', or 'ROLE_ROOT' can execute it.</li>
     *     <li>Proper logging of administrative actions should be in place when this method is called.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * // As an admin:
     * userService.adminSetUserEnabled(101L, false); // Disable account
     * System.out.println("User 101 account disabled by admin.");
     *
     * userService.adminSetUserEnabled(101L, true); // Enable account
     * System.out.println("User 101 account enabled by admin.");
     * }</pre>
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    void adminSetUserEnabled(Long userId, boolean enabled);

    /**
     * <p><b>Purpose:</b> To permanently delete a user account and all associated data.</p>
     * <p>This is a highly sensitive operation that should be handled with extreme care.
     * It removes the user's record from the system, which is typically irreversible.
     * Depending on data retention policies, some data might be soft-deleted or archived.</p>
     *
     * @param userId The unique identifier of the user account to be deleted.
     * @throws IllegalArgumentException if {@code userId} is null or invalid.
     * @throws ResourceNotFoundException if no user with the given {@code userId} is found.
     * @throws AccessDeniedException if the authenticated user is not authorized to delete the account
     *                               (e.g., not the user themselves, or not an admin).
     *
     * <p><b>Important Notes:</b></p>
     * <ul>
     *     <li>This method is {@code @Transactional} to ensure that all related data deletions
     *         are atomic.</li>
     *     <li>Authorization for this method is critical. It should typically be callable only by:
     *         <ul>
     *             <li>The user themselves (self-service deletion).</li>
     *             <li>An administrator with appropriate roles (e.g., 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT').</li>
     *         </ul>
     *         The {@code @PreAuthorize} annotation might be applied here, or authorization can be handled
     *         at the controller level (e.g., by checking {@code @AuthenticationPrincipal} against {@code userId}).
     *     </li>
     *     <li>Consider implementing a "soft delete" mechanism if data recovery or auditing is required.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * // As the user themselves (via a controller endpoint that validates current user ID):
     * userService.deleteUserAccount(currentUser.getId());
     * System.out.println("User account deleted successfully.");
     *
     * // As an admin:
     * userService.deleteUserAccount(222L);
     * System.out.println("User 222 account deleted by admin.");
     * }</pre>
     */
    @Transactional
    void deleteUserAccount(Long userId);

    /**
     * <p><b>Purpose:</b> To retrieve the currently authenticated user's details.</p>
     * <p>This method provides a convenient way to access the profile information of the user
     * who is currently logged into the system. It typically relies on Spring Security's
     * authentication context to identify the principal.</p>
     *
     * @return The {@link User} object representing the currently authenticated user.
     * @throws IllegalStateException if no user is currently authenticated or if the authenticated
     *                               principal cannot be resolved to a {@link User} object.
     * @throws ResourceNotFoundException if the authenticated user's ID does not correspond to an existing user in the database.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * try {
     *     User currentUser = userService.getCurrentUser();
     *     System.out.println("Welcome, " + currentUser.getNickname() + "!");
     * } catch (IllegalStateException e) {
     *     System.err.println("No user is currently authenticated.");
     * }
     * }</pre>
     */
    User getCurrentUser();

}
