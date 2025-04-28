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
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    //    List<RevokedToken> findByUsername(String username);

    //    Page<RevokedToken> findByUsername(String username, Pageable pageable);

    Optional<RevokedToken> findByTokenId(String tokenId);

    boolean existsByTokenId(String tokenId);

    Optional<RevokedToken> findByTokenIdAndRevokedAtIsNotNull(String tokenId);

    boolean existsByTokenIdAndRevokedAtIsNotNull(String tokenId);

    /**
     * Marks a specific token as revoked by setting its revokedAt timestamp and reason,
     * only if it hasn't been revoked already (revokedAt is null).
     *
     * @param tokenId   The unique identifier of the token to revoke.
     * @param revokedAt The timestamp when the revocation occurred.
     * @param reason    The reason for revocation.
     * @return The number of tokens updated (should be 0 or 1).
     */
    @Modifying // Indicate this query modifies data
    @Query("UPDATE RevokedToken rt SET rt.revokedAt = :revokedAt, rt.reason = :reason WHERE rt.tokenId = :tokenId AND rt.revokedAt IS NULL")
    int updateRevokedAtAndReasonByTokenIdAndRevokedAtIsNull(@Param("tokenId") String tokenId, @Param("revokedAt") Instant revokedAt, @Param("reason") String reason);

    int deleteByRevokedAtBefore(Instant cutoff);

}

