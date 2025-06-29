/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.config.properties;

import jakarta.validation.constraints.Min;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "auth.aspect")
@Validated
public class AuthenticationAspectProperties {
    @NonNull
    private Duration cacheTimeout = Duration.ofHours(1);

    @Min(1)
    private int maxRetryAttempts = 3;

    @NonNull
    private Duration retryInterval = Duration.ofSeconds(1);

    // getters and setters

    public @NonNull Duration getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(@NonNull Duration cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public @NonNull Duration getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(@NonNull Duration retryInterval) {
        this.retryInterval = retryInterval;
    }
}
