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

    // Find by email (which is the username)
    // Eagerly fetch authorities and passkeyCredentials to avoid N+1 issues
    // when UserDetails is loaded.
    // Use @EntityGraph to specify that 'authorities' should be fetched eagerly
    @EntityGraph(attributePaths = {"authorities", "passkeyCredentials"})
    // Eagerly fetch authorities and passkeys
    Optional<User> findByEmail_Value(String emailValue);

    // Find by email, excluding soft-deleted users
    @EntityGraph(attributePaths = {"authorities", "passkeyCredentials"})
    Optional<User> findByEmail_ValueAndDeletedAtIsNull(String email);

    // Find by externalId (user handle for WebAuthn)
    @EntityGraph(attributePaths = {"authorities", "passkeyCredentials"})
    // Eagerly fetch authorities and passkeys
    Optional<User> findByExternalId(String externalId);

    // Find by externalId, excluding soft-deleted users
    @EntityGraph(attributePaths = {"authorities", "passkeyCredentials"})
    Optional<User> findByExternalIdAndDeletedAtIsNull(String externalId);

    /**
     * Finds a user by their authServerSubjectId.
     * This ID is the 'sub' claim from the JWT issued by the authserver,
     * linking the local amlume-shop user to the central authserver identity.
     *
     * @param authServerSubjectId The subject ID from the authserver.
     * @return An Optional containing the User if found, or empty otherwise.
     */
    Optional<User> findBy(String authServerSubjectId);

    // Check if a user exists by email
    boolean existsByEmail_Value(String email);

    // Check if a user exists by externalId
    boolean existsByExternalId(String externalId);

    // Check if a user exists by recovery email blind index
    boolean existsByRecoveryEmailBlindIndex(String recoveryEmailBlindIndex);

    // Check if a user exists by recovery email blind index, excluding a specific user ID
    boolean existsByRecoveryEmailBlindIndexAndIdNot(String recoveryEmailBlindIndex, Long userId);

    // If we need to find a user by their recovery email (e.g., for account recovery flows)
    @EntityGraph(attributePaths = "authorities")
    Optional<User> findByRecoveryEmailBlindIndex(String recoveryEmailBlindIndex);


    @EntityGraph(attributePaths = "authorities")
    Optional<User> findByRecoveryEmailBlindIndexAndDeletedAtIsNull(String recoveryEmailBlindIndex);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.failedLoginAttempts = :attempts, u.accountStatus.lockoutExpirationTime = :lockoutTime WHERE u.id = :userId")
    void updateFailedLoginAndLockout(@Param("userId") Long userId, @Param("attempts") int attempts, @Param("lockoutTime") Instant lockoutTime);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.failedLoginAttempts = :attempts WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, @Param("attempts") int attempts);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.lockoutExpirationTime = :lockoutTime, u.accountStatus.failedLoginAttempts = 0 WHERE u.id = :userId")
    void updateAccountLockStatus(@Param("userId") Long userId, @Param("lockoutTime") Instant lockoutTime);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginTime WHERE u.id = :userId")
    void updateLastLoginTime(@Param("userId") Long userId, @Param("lastLoginTime") Instant lastLoginTime);

    // You might add other specific update methods if needed, e.g., for enabling/disabling account.
    @Modifying
    @Query("UPDATE User u SET u.accountStatus.enabled = :enabled WHERE u.id = :userId")
    void updateUserEnabledStatus(@Param("userId") Long userId, @Param("enabled") boolean enabled);

    // Optional<User> findByName(String username); // REMOVED as 'name' field is replaced by givenName/surname

    // Consider adding if needed:
    // Optional<User> findBygivenNameAndSurname(String givenName, String surname);
    // List<User> findBygivenName(String givenName);
    // List<User> findBySurname(String surname);
    // Optional<User> findByNickname(String nickname);

//    Optional<User> findByMobileNumber(String mobileNumber); // This would be findByMobileNumberE164Value
//
//    Optional<User> findByEmailOrMobileNumber(String email, String mobileNumber);

}
