/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.login.rate-limit")
public class LoginRateLimitProperties {
    @NotNull
    private Duration cleanupInterval = Duration.ofHours(1);

    @Min(1)
    private final int maxAttempts = 100;

    @NotNull
    private final Duration timeWindowMinutes = Duration.ofMinutes(15);

    @Min(1)
    private final long capacity = 100;

    @NotNull
    private final Duration refillPeriod = Duration.ofMinutes(1);

//    @Min(1)
//    private final int duration = 1;

    private TimeUnit timeUnit = TimeUnit.MINUTES;

    private boolean failOpen = false;

    private boolean enabled = true;
}
