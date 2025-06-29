/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.repository;

import me.amlu.authserver.user.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address's value, excluding soft-deleted users.
     * Eagerly fetches roles and passkeyCredentials to avoid N+1 issues.
     *
     * @param emailValue The string value of the email address to search for.
     * @return An Optional containing the User if found.
     */
    @EntityGraph(attributePaths = {"roles", "passkeyCredentials"})
    Optional<User> findByEmail_ValueAndDeletedAtIsNull(String emailValue);

    /**
     * Finds a user by their external ID (user handle for WebAuthn), excluding soft-deleted users.
     * Eagerly fetches roles and passkeyCredentials.
     *
     * @param externalId The external ID to search for.
     * @return An Optional containing the User if found.
     */
    @EntityGraph(attributePaths = {"roles", "passkeyCredentials"})
    Optional<User> findByExternalIdAndDeletedAtIsNull(String externalId);

    /**
     * Checks if a user exists with the given email address value.
     *
     * @param emailValue The email string to check.
     * @return true if a user with the email exists, false otherwise.
     */
    boolean existsByEmail_Value(String emailValue);

    /**
     * Checks if a user exists with the given external ID.
     *
     * @param externalId The external ID to check.
     * @return true if a user with the external ID exists, false otherwise.
     */
    boolean existsByExternalId(String externalId);

    /**
     * Checks if a user exists with the given recovery email blind index.
     *
     * @param recoveryEmailBlindIndex The blind index to check.
     * @return true if a user with the blind index exists, false otherwise.
     */
    boolean existsByRecoveryEmailBlindIndex(String recoveryEmailBlindIndex);

    /**
     * Finds a user by their recovery email blind index, excluding soft-deleted users.
     *
     * @param recoveryEmailBlindIndex The blind index to search for.
     * @return An Optional containing the User if found.
     */
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByRecoveryEmailBlindIndexAndDeletedAtIsNull(String recoveryEmailBlindIndex);

    /**
     * Finds a user by their ID, excluding soft-deleted users.
     *
     * @param id The user ID.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.failedLoginAttempts = :attempts, u.accountStatus.lockoutExpirationTime = :lockoutTime WHERE u.id = :userId")
    void updateFailedLoginAndLockout(@Param("userId") Long userId, @Param("attempts") int attempts, @Param("lockoutTime") Instant lockoutTime);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.failedLoginAttempts = :attempts WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, @Param("attempts") int attempts);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginTime WHERE u.id = :userId")
    void updateLastLoginTime(@Param("userId") Long userId, @Param("lastLoginTime") Instant lastLoginTime);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.enabled = :enabled WHERE u.id = :userId")
    void updateUserEnabledStatus(@Param("userId") Long userId, @Param("enabled") boolean enabled);

    boolean existsByRecoveryEmailBlindIndexAndIdNot(String newRecoveryBlindIndex, Long userId);
}
