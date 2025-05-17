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
    Optional<User> findByUserId(Long userId);

    // --- Finders based on Embedded Objects ---

    /**
     * Finds a user by their email address stored within the ContactInfo.UserEmail embedded object.
     * This email is considered the primary email for the amlume-shop user.
     *
     * @param email The email address to search for.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByContactInfoUserEmailEmail(String email);

    /**
     * Finds a user by their authServerSubjectId.
     * This ID is the 'sub' claim from the JWT issued by the authserver,
     * linking the local amlume-shop user to the central authserver identity.
     *
     * @param authServerSubjectId The subject ID from the authserver.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByAuthServerSubjectId(String authServerSubjectId);

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email The email address to check.
     * @return true if a user with the email exists, false otherwise.
     */
    boolean existsByContactInfoUserEmailEmail(String email);

    /**
     * Checks if a user exists with the given authServerSubjectId.
     *
     * @param authServerSubjectId The subject ID from the authserver.
     * @return true if a user with this authServerSubjectId exists, false otherwise.
     */
    boolean existsByAuthServerSubjectId(String authServerSubjectId);


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
     * Updates the last login timestamp for a user.
     * This tracks amlume-shop specific access time.
     *
     * @param userId The ID of the user to update.
     * @param now    The current timestamp to set as the last login timestamp.
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountStatus.lastLoginTime = :now WHERE u.userId = :userId")
    void updateLastLoginTime(@Param("userId") Long userId, @Param("now") Instant now);

}
