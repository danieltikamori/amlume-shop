/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.repository;

import me.amlu.shop.amlume_shop.security.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    void deleteByExpirationDateBefore(Instant date);
//    List<RevokedToken> findByUsername(String username);
    boolean existsByTokenId(String tokenId);

//    boolean existsByToken(String token);

    // Using this in the new revokeAllUserTokens method
    /**
     * Updates the revokedAt and reason fields of a RevokedToken entity
     * for a given username and a null value for the revokedAt field.
     *rt.revokedAt IS NULL Is Essential to prevent updating tokens that have already been revoked.
     * This is to prevent the same token from being revoked multiple times.
     * Idempotency:
     *     The rt.revokedAt IS NULL condition ensures that the update operation is idempotent. This means that if you call the revokeAllUserTokens method multiple times for the same user, it will only update the tokens that have not already been revoked.
     *     Without this condition, each subsequent call would update all tokens, potentially overwriting the revokedAt and reason values.
     * Concurrency:
     *     In concurrent environments, multiple threads or processes might attempt to revoke tokens for the same user simultaneously.
     *     The rt.revokedAt IS NULL condition prevents race conditions and ensures that only one thread or process successfully revokes a token.
     * Data Integrity:
     *     It maintains the integrity of the revokedAt field, which should represent the actual time when the token was revoked.
     *
     * @param username
     * @param revokedAt
     * @param reason
     */

    @Modifying
    @Query("UPDATE RevokedToken rt SET rt.revokedAt = :revokedAt, rt.reason = :reason WHERE rt.username = :username AND rt.revokedAt IS NULL")
    void updateRevokedAtAndReasonByUsernameAndRevokedAtIsNull(String username, Instant revokedAt, String reason);

    void deleteByRevokedAtBefore(Instant instant);
}
