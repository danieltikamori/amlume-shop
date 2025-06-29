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

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Component
@ConfigurationProperties(prefix = "token")
public class TokenProperties {

    private Duration accessValidity;
    private Duration refreshValidity;
    private double rateLimit;
    private double claimsValidationRateLimitPermitsPerSecond;
    private double validationRateLimitPermitsPerSecond;
    private int validationRateLimitMaxAttempts;
    private int validationRateLimitWindowSeconds;
    private int validationRateLimitWindowSizeInSeconds;

//    @Value("${token.access.validity}")
//    private long accessTokenValidity;
//
//    @Value("${token.refresh.validity}")
//    private long refreshTokenValidity;
//
//    @Value("${token.rate.limit}")
//    private double rateLimit;
//
//    @Value("${rateLimit.token.validation}")
//    private double tokenValidationRateLimiterValue;
//
//    @Value("${rateLimit.claims.validation}")
//    private double claimsValidationRateLimiterValue;
}
