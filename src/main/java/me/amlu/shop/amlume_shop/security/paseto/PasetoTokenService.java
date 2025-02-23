/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto;

import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;

public interface PasetoTokenService {
    String generatePublicAccessToken(String username, Duration validity) throws TokenGenerationFailureException;

    String generateLocalAccessToken(String username, Duration validity) throws TokenGenerationFailureException;

    String generateRefreshToken(User user);

    Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException;

    Map<String, Object> validateLocalAcessToken(String token) throws TokenValidationFailureException;

    Map<String, Object> validateRefreshToken(String token) throws TokenValidationFailureException;

    PasetoClaims createAccessPasetoClaims(String username, Duration validity);

    PasetoClaims createRefreshPasetoClaims(String userId, Duration validity);

    PasetoClaims createPasetoFooterClaims(String keyId, String wrappedPaserk);

    void validateAccessTokenClaims(Map<String, Object> claims) throws TokenValidationFailureException;

    void validateRefreshTokenClaims(Map<String, Object> claims) throws TokenValidationFailureException;

    void validatePayload(String payload) throws TokenGenerationFailureException;

    boolean isTokenExpired(String token);

//    @Transactional
//    void revokeToken(String token);

    @Transactional
    void revokeToken(String token, String reason);

    @Transactional
    void revokeAllUserTokens(String name, String reason);
}

