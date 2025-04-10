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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.lang.ScopedValue;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(Long userId);

    boolean existsByUsername(String username);

    User findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username);

    Optional<User> findByRefreshToken(String hashpw);

//    Optional<User> findByIdWithRoles(Long id); // Possibly unnecessary because we are using fetch = FetchType.EAGER in the User class.

//    * **`@Modifying`:** Indicates that the query will modify data.
//    * **`@Query`:** Defines the JPQL query to execute.
//    * **`@Param`:** Binds the method parameters to the query parameters.

    @Query("SELECT r FROM User u JOIN u.roles r WHERE u.userId = :userId")
    Set<UserRole> findRolesByUserId(@Param("userId") Long userId);

    boolean existsByContactInfoEmail(String email);

//    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.contactInfo.email = :email")
//    boolean existsByEmail(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.failedLoginAttempts = :failedLoginAttempts WHERE u.userId = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, @Param("failedLoginAttempts") int failedLoginAttempts);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.accountNonLocked = :accountNonLocked, u.accountStatus.lockTime = :lockTime WHERE u.userId = :userId")
    void updateAccountLockStatus(@Param("userId") Long userId, @Param("accountNonLocked") boolean accountNonLocked, @Param("lockTime") Instant lockTime);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.accountNonLocked = true, u.accountStatus.lockTime = null, u.accountStatus.failedLoginAttempts = 0 WHERE u.userId = :userId")
    void unlockUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.authenticationInfo.password = :password WHERE u.userId = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("password") String password);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = :refreshToken WHERE u.userId = :userId")
    void updateRefreshToken(@Param("userId") Long userId, @Param("refreshToken") String refreshToken);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = null WHERE u.userId = :userId")
    void clearRefreshToken(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.accountStatus.lastLoginTime = :now WHERE u.userId = :userId")
    void updateLastLoginTime(@Param("userId") Long userId, @Param("now") Instant now);

    void update(User profile);

    UserDetails findUserDetails(String userId);

    @Query("SELECT new me.amlu.shop.amlume_shop.model.AuthenticationInfo(" +
            "u.username, u.password, u.enabled) " +
            "FROM User u WHERE u.username = :username")
    AuthenticationInfo findAuthenticationInfoByUsername(@Param("username") String username);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET " +
            "u.password = :#{#newInfo.password}, " +
            "u.enabled = :#{#newInfo.enabled} " +
            "WHERE u.username = :username")
    void updateAuthenticationInfo(@Param("username") String username,
                                  @Param("newInfo") AuthenticationInfo newInfo);

    boolean existsByAuthenticationInfoUsername(@NotBlank @Size(min = 3, max = 20) String username);

    boolean existsByContactInfoUserEmailEmail(@Email @Size(min = 5, max = 50) String userEmail);

    Optional<User> findByAuthenticationInfoUsername(String username);

    Optional<User> findByContactInfoUserEmailEmail(String email);

    Optional<User> findByAuthenticationInfoUsernameOrContactInfoUserEmailEmail(String usernameOrEmail, String usernameOrEmail1);
}