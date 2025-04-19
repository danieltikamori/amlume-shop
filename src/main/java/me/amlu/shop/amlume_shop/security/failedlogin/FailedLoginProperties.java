/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

// package me.amlu.shop.amlume_shop.config.properties;
    package me.amlu.shop.amlume_shop.security.failedlogin;

    import jakarta.validation.constraints.Min;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import lombok.Data;
    import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.validation.annotation.Validated;

    import java.time.Duration;

    @Configuration
    @ConfigurationProperties(prefix = "security.failed-login")
    @Validated
    @Data
    public class FailedLoginProperties {

        @Min(1)
        private int maxAttempts = 5; // Max attempts before blocking

        @NotNull
        private Duration counterWindow = Duration.ofMinutes(15); // How long to keep the failure count

        // Optional: Add lockout duration if implementing timed lockouts
        // @NotNull
        // private Duration lockoutDuration = Duration.ofHours(1);

        @NotBlank
        private String redisKeyPrefix = "failedlogin:attempts:";
    }
    