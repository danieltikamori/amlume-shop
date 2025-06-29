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
// Removed: import me.amlu.shop.amlume_shop.user_management.User;

/**
 * Interface defining the contract for generating different types of PASETO tokens (v4).
 * Implementations are responsible for the actual signing (public) or encryption (local)
 * process using the appropriate keys and provided claims.
 */
public interface TokenGenerationService {

    /**
     * Generates a v4.public PASETO access token by signing the payload.
     * Requires a private key for signing.
     *
     * @param claims The PasetoClaims object containing payload and footer claims. Must not be null.
     * @return The generated PASETO token string (v4.public...).
     * @throws TokenGenerationFailureException if token generation fails.
     * @throws NullPointerException if claims is null.
     */
    String generatePublicAccessToken(PasetoClaims claims) throws TokenGenerationFailureException;

    /**
     * Generates a v4.local PASETO access token by encrypting the payload.
     * Requires a symmetric secret key for encryption.
     *
     * @param claims The PasetoClaims object containing payload and footer claims. Must not be null.
     * @return The generated PASETO token string (v4.local...).
     * @throws TokenGenerationFailureException if token generation fails.
     * @throws NullPointerException if claims is null.
     */
    String generateLocalAccessToken(PasetoClaims claims) throws TokenGenerationFailureException;

    /**
     * Generates a v4.local PASETO refresh token by encrypting the payload.
     * Requires a symmetric secret key (typically different from the access token key) for encryption.
     *
     * @param claims The PasetoClaims object containing payload and footer claims for the refresh token. Must not be null.
     * @return The generated PASETO token string (v4.local...).
     * @throws TokenGenerationFailureException if token generation fails.
     * @throws NullPointerException if claims is null.
     */
    String generateLocalRefreshToken(PasetoClaims claims) throws TokenGenerationFailureException;

    // Removed generateLocalRefreshToken(User user) signature.
    // Claim creation logic belongs upstream (e.g., in TokenClaimsService or PasetoTokenServiceImpl).
    // This interface focuses solely on the PASETO generation step given pre-built claims.
}
