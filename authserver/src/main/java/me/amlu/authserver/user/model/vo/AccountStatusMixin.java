/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.model.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Jackson Mixin for the {@link AccountStatus} value object.
 * This mixin provides a constructor annotated with {@link JsonCreator} to guide Jackson
 * during deserialization, especially when dealing with Spring Security's type allowlisting.
 */
public abstract class AccountStatusMixin {

    /**
     * Constructor used by Jackson for deserializing {@link AccountStatus} objects.
     *
     * @param enabled               Whether the account is enabled.
     * @param failedLoginAttempts   The number of failed login attempts.
     * @param lockoutExpirationTime The time until which the account is locked out, or null if not locked.
     * @param accountNonExpired     Whether the account is non-expired.
     * @param credentialsNonExpired Whether the credentials are non-expired.
     */
    @JsonCreator
    public AccountStatusMixin(
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("failedLoginAttempts") int failedLoginAttempts,
            @JsonProperty("lockoutExpirationTime") Instant lockoutExpirationTime,
            @JsonProperty("accountNonExpired") boolean accountNonExpired,
            @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired
            // Note: Other fields like lastLoginTime or derived properties (e.g., accountNonLocked)
            // are typically not part of the core constructor for deserialization if they are
            // managed by behavior or are transient. This constructor should match the
            // primary way AccountStatus objects are instantiated with their persistent state.
    ) {
        // Mixin constructors are abstract and don't have bodies.
    }
}
