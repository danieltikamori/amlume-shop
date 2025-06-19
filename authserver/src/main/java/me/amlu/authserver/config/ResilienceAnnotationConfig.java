/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for Resilience4j annotation-based resilience patterns.
 * <p>
 * This class enables the use of Resilience4j annotations like {@code @CircuitBreaker},
 * {@code @Retry}, {@code @Bulkhead}, and {@code @TimeLimiter} in the application.
 * </p>
 * <p>
 * Spring Cloud Circuit Breaker automatically configures the necessary aspects
 * when the spring-cloud-starter-circuitbreaker-resilience4j dependency is present.
 * </p>
 *
 * @see io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
 * @see io.github.resilience4j.retry.annotation.Retry
 * @see io.github.resilience4j.bulkhead.annotation.Bulkhead
 * @see io.github.resilience4j.timelimiter.annotation.TimeLimiter
 */
@Configuration
@EnableAspectJAutoProxy
public class ResilienceAnnotationConfig {
    // Spring Cloud Circuit Breaker auto-configures the necessary aspects
    // No manual bean definitions needed
}
