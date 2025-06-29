/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.oauth2.repository;

import me.amlu.authserver.oauth2.model.OAuth2Authorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuth2AuthorizationRepository extends JpaRepository<OAuth2Authorization, String> {

    Optional<OAuth2Authorization> findByState(String state);

    Optional<OAuth2Authorization> findByAuthorizationCodeValue(String authorizationCode);

    Optional<OAuth2Authorization> findByAccessTokenValue(String accessToken);

    Optional<OAuth2Authorization> findByRefreshTokenValue(String refreshToken);

    Optional<OAuth2Authorization> findByUserCodeValue(String userCode);

    Optional<OAuth2Authorization> findByDeviceCodeValue(String deviceCode);

    // Query to find authorization by token value and type (approximating token lookup)
    @Query("SELECT a FROM OAuth2Authorization a WHERE " +
            "(a.state = :token OR " +
            "a.authorizationCodeValue = :token OR " +
            "a.accessTokenValue = :token OR " +
            "a.refreshTokenValue = :token OR " +
            "a.userCodeValue = :token OR " +
            "a.deviceCodeValue = :token)")
    Optional<OAuth2Authorization> findByTokenValue(@Param("token") String token);

    // More specific queries might be needed depending on how JpaOAuth2AuthorizationService is implemented.
    // For example, finding by a specific token type and its value:
    @Query("SELECT a FROM OAuth2Authorization a WHERE " +
            "(:tokenType = 'state' AND a.state = :token) OR " +
            "(:tokenType = 'code' AND a.authorizationCodeValue = :token) OR " +
            "(:tokenType = 'access_token' AND a.accessTokenValue = :token) OR " +
            "(:tokenType = 'refresh_token' AND a.refreshTokenValue = :token) OR " +
            "(:tokenType = 'user_code' AND a.userCodeValue = :token) OR " +
            "(:tokenType = 'device_code' AND a.deviceCodeValue = :token)")
    Optional<OAuth2Authorization> findByTokenValueAndTokenType(@Param("token") String token, @Param("tokenType") String tokenType);

    void deleteByPrincipalName(String principalName);

    @Modifying // Necessary for delete operations
    @Query("DELETE FROM OAuth2Authorization a WHERE a.principalName = :principalName")
    void deleteAllByPrincipalName(@Param("principalName") String principalName);
}
