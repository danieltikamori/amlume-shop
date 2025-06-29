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

import java.util.Map;

/**
 * Interface defining operations for caching token claims.
 * The underlying implementation (e.g., in-memory, Redis/Valkey) may vary,
 * but the contract remains consistent.
 */
public interface TokenCacheService {

    /**
     * Stores the claims associated with a specific token string in the cache.
     * The entry will typically have a Time-To-Live (TTL) configured in the
     * implementation based on application properties.
     *
     * @param token  The token string (used as the cache key). Must not be null or blank.
     * @param claims The map of claims associated with the token. Must not be null.
     */
    void putToken(String token, Map<String, Object> claims);

    /**
     * Retrieves the claims associated with a specific token string from the cache.
     *
     * @param token The token string (used as the cache key). Must not be null or blank.
     * @return The map of claims if found in the cache and not expired, otherwise {@code null}.
     */
    Map<String, Object> getTokenClaims(String token);

    /**
     * Removes the cached claims associated with a specific token string.
     * If the token is not found in the cache, the operation completes silently.
     *
     * @param token The token string (used as the cache key). Must not be null or blank.
     */
    void removeToken(String token);

    /**
     * Clears the entire token cache.
     * Use with caution, as this removes all cached token claims.
     * The exact mechanism (e.g., iterating keys, pattern deletion) depends
     * on the underlying cache implementation.
     */
    void clearCache();
}
