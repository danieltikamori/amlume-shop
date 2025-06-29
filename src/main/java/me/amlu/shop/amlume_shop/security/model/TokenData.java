/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record TokenData(
        String token,
        Map<String, Object> claims,
        Instant expirationTime
) {
    /**
     * Constructor with validation
     */
    public TokenData {
        // Defensive copy of mutable map
        claims = Map.copyOf(claims);

        // Null checks
        Objects.requireNonNull(token, "Token cannot be null");
        Objects.requireNonNull(claims, "Claims cannot be null");
        Objects.requireNonNull(expirationTime, "Expiration time cannot be null");
    }

    /**
     * Safe getter for claims that returns an unmodifiable copy
     */
    @Override
    public Map<String, Object> claims() {
        return Collections.unmodifiableMap(claims);
    }

    // TOCHECK
    public static Object getApproximateSize(Object o) {
        return switch (o) {
            case String string -> string.length();
            case Map<?, ?> map -> map.size();
            default -> 0;
        };
    }
}
