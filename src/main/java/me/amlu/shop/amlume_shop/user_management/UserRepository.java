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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // --- Standard Finders ---
    Optional<User> findByUserId(Long userId); // Keep standard findById if preferred

    // --- Finders based on Embedded Objects ---

    /**
     * Finds a user by their username string stored within the AuthenticationInfo.Username embedded object.
     * Uses property expression: authenticationInfo.username.username (assuming Username VO has a 'username' field)
     *
     * @param username The username string to search for.
     * @return An Optional containing the User if found.
     */
    // Optional<User> findByAuthenticationInfoUsername(Username authenticationInfo_username); // OLD
    Optional<User> findByAuthenticationInfoUsername_Username(String username); // NEW: Query by nested property

    /**
     * Finds a user by their email address stored within the ContactInfo.UserEmail embedded object.
     *
     * @param email The email address to search for.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByContactInfoUserEmailEmail(String email);

    /**
     * Finds a user by either their username string or email address.
     * Uses property expression: authenticationInfo.username.username
     *
     * @param username The username string to search for.
     * @param email    The email address to search for.
     * @return An Optional containing the User if found by either identifier.
     */
    // Optional<User> findByAuthenticationInfoUsernameOrContactInfoUserEmailEmail(String username, String email); // OLD (Username part was wrong)
    Optional<User> findByAuthenticationInfoUsername_UsernameOrContactInfoUserEmailEmail(String username, String email); // NEW: Correct username path


    // --- Existence Checks based on Embedded Objects ---

    /**
     * Checks if a user exists with the given username string.
     * Uses property expression: authenticationInfo.username.username
     *
     * @param username The username string to check.
     * @return true if a user with the username exists, false otherwise.
     */
    // boolean existsByAuthenticationInfoUsername(String username); // OLD (Ambiguous if it expected String or Username object)
    boolean existsByAuthenticationInfoUsername_Username(String username); // NEW: Query by nested property

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email The email address to check.
     * @return true if a user with the email exists, false otherwise.
     */
    boolean existsByContactInfoUserEmailEmail(String email);

    // --- Custom Queries ---

    /**
     * Fetches the roles associated with a specific user ID.
     * Note: EAGER fetching on User.roles might make this less necessary.
     *
     * @param userId The ID of the user.
     * @return A Set of UserRole objects associated with the user.
     */
    @Query("SELECT r FROM User u JOIN u.roles r WHERE u.userId = :userId")
    Set<UserRole> findRolesByUserId(@Param("userId") Long userId);

    // --- Targeted Update Queries (@Modifying) ---
    // These are generally more efficient than loading the full entity for simple updates.

    /**
     * Updates the failed login attempt count for a user.
     *
     * @param userId              The ID of the user to update.
     * @param failedLoginAttempts The new count of failed attempts.
     */
    @Modifying
    @Transactional // Add Transactional if called outside a transactional service method
    @Query("UPDATE User u SET u.accountStatus.failedLoginAttempts = :failedLoginAttempts WHERE u.userId = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, @Param("failedLoginAttempts") int failedLoginAttempts);

    /**
     * Updates the account lock status and lock timestamp for a user.
     *
     * @param userId        The ID of the user to update.
     * @param accountNonLocked The new lock status (true means NOT locked, false means locked).
     * @param lockTime      The timestamp when the lock was applied (null if unlocking).
     */
    @Modifying
    @Transactional // Add Transactional if called outside a transactional service method
    @Query("UPDATE User u SET u.accountStatus.accountNonLocked = :accountNonLocked, u.accountStatus.lockTime = :lockTime WHERE u.userId = :userId")
    void updateAccountLockStatus(@Param("userId") Long userId, @Param("accountNonLocked") boolean accountNonLocked, @Param("lockTime") Instant lockTime);


    /**
     * Unlocks a user account by setting locked status to true (not locked), clearing lock timestamp, and resetting failed attempts.
     *
     * @param userId The ID of the user to unlock.
     */
    @Modifying
    @Transactional // Add Transactional if called outside a transactional service method
    @Query("UPDATE User u SET u.accountStatus.accountNonLocked = true, u.accountStatus.lockTime = null, u.accountStatus.failedLoginAttempts = 0 WHERE u.userId = :userId")
    void unlockUser(@Param("userId") Long userId);

    /**
     * Updates the user's password.
     * IMPORTANT: The newPassword provided MUST be already encoded/hashed.
     *
     * @param userId      The ID of the user whose password is to be updated.
     * @param newPassword The new, encoded password (as a UserPassword object).
     */
    @Modifying
    @Transactional // Add Transactional if called outside a transactional service method
    @Query("UPDATE User u SET u.authenticationInfo.password = :newPassword WHERE u.userId = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("newPassword") UserPassword newPassword);

    /**
     * Updates the last login timestamp for a user.
     *
     * @param userId The ID of the user to update.
     * @param now    The current timestamp to set as the last login timestamp.
     */
    @Modifying
    @Transactional // Add Transactional if called outside a transactional service method
    @Query("UPDATE User u SET u.accountStatus.lastLoginTime = :now WHERE u.userId = :userId")
    void updateLastLoginTime(@Param("userId") Long userId, @Param("now") Instant now);


    // --- Potentially Unused / To Be Reviewed ---

    // Optional<User> findByRefreshToken(String hashpw); // Review if refresh tokens are stored differently

    // @Modifying
    // @Query("UPDATE User u SET u.refreshToken = :refreshToken WHERE u.userId = :userId")
    // void updateRefreshToken(@Param("userId") Long userId, @Param("refreshToken") String refreshToken); // Review: User entity doesn't show refreshToken field

    // @Modifying
    // @Query("UPDATE User u SET u.refreshToken = null WHERE u.userId = :userId")
    // void clearRefreshToken(@Param("userId") Long userId); // Review: User entity doesn't show refreshToken field

    // UserDetails findUserDetails(Long userId); // Review if needed, findById returns User which is UserDetails

    // --- Removed Methods ---
    // Removed existsByUsername, findByEmail, findByUsername, findByUsernameOrEmail (use embedded object paths)
    // Removed existsByContactInfoEmail (use existsByContactInfoUserEmailEmail)
    // Removed findAuthenticationInfoByUsername (conflicting AuthenticationInfo class)
    // Removed updateAuthenticationInfo (conflicting AuthenticationInfo class)
    // Removed update(User profile) (non-standard JPA method)
}