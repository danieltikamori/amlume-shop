/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.amlu.shop.amlume_shop.payload.ErrorResponse;
import me.amlu.shop.amlume_shop.ratelimiter.RateLimiter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static me.amlu.shop.amlume_shop.commons.Constants.IP_HEADERS;

/**
 * A global rate-limiting filter that restricts the number of requests a client can make
 * within a specific time window. This filter is applied to all incoming requests.
 *
 * <p>It uses a {@link RateLimiter} implementation (e.g., Redis-based) to enforce rate limits
 * and sends appropriate error responses when limits are exceeded.</p>
 * <p>
 * Considerations:
 * •Trusting IP Headers: The accuracy of getClientIp depends on the infrastructure setup. If headers like X-Forwarded-For can be spoofed before reaching your application, the rate limiting might be inaccurate or bypassed. Ensure your load balancer/proxy setup correctly sets/overwrites these headers.
 * •"global" Limiter Configuration: This filter uses the key "global:" + clientIp. Ensure you have a corresponding configuration in application.yml under rate-limiter.limiters.global or that the rate-limiter.defaults provide the desired behavior for this global limit.
 * •Granularity: This filter provides global IP-based limiting. For more granular limits (e.g., per user per endpoint), you would typically use different keys and potentially apply rate limiting via AOP on specific service methods or controller endpoints, possibly in addition to this global filter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
// Run very early, but potentially after CORS/logging filters if they have HIGHEST_PRECEDENCE
public class GlobalRateLimitingFilter extends OncePerRequestFilter {

    private final String globalLimiterName;
    private static final Logger log = LoggerFactory.getLogger(GlobalRateLimitingFilter.class);

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@code GlobalRateLimitingFilter}.
     *
     * @param rateLimiter  the {@link RateLimiter} implementation to use for rate limiting
     * @param objectMapper the {@link ObjectMapper} for serializing error responses
     */
    // Inject the specific RateLimiter bean you configured (likely the Redis one)
    public GlobalRateLimitingFilter(@Value("${rate-limiter.global-filter-limiter-name}") String globalLimiterName,
                                    @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter,
                                    ObjectMapper objectMapper) {
        this.globalLimiterName = globalLimiterName;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        log.info("GlobalRateLimitingFilter initialized, using limiter name: '{}'.", globalLimiterName);
    }

    /**
     * Filters incoming requests to enforce rate limits. If the rate limit is exceeded,
     * an error response is sent, and the request is not processed further.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain to pass the request/response to the next filter
     * @throws ServletException if an error occurs during filtering
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        // Use a generic key prefix for global limiting from injected property
        String rateLimitKey = globalLimiterName + ":" + clientIp;

        try {
            if (!rateLimiter.tryAcquire(rateLimitKey)) {
                log.warn("Global rate limit exceeded for IP: {}", clientIp);
                sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many requests from your IP address. Please try again later.");
                return; // Stop the filter chain
            }
        } catch (Exception e) {
            // Handle potential errors from the rate limiter (e.g., Redis connection issues)
            // Decide whether to fail open (allow request) or fail closed (deny request) based on 'rate-limiter.fail-open' property
            // For now, let's log and deny (fail closed) for security.
            log.error("Error checking rate limit for IP: {}. Denying request.", clientIp, e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "RATE_LIMIT_ERROR", "Could not verify request rate limit. Please try again.");
            return; // Stop the filter chain
        }

        // Rate limit check passed, proceed with the chain
        filterChain.doFilter(request, response);
    }

    /**
     * Retrieves the client IP address from the request headers or falls back to the remote address.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    // Helper to get Client IP (reuse logic from other classes if available)
    private String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs (take the first one)
                return ip.contains(",") ? ip.split(",")[0].trim() : ip.trim();
            }
        }
        return request.getRemoteAddr(); // Fallback
    }

    /**
     * Sends a standardized error response with the given status, error code, and message.
     *
     * @param response  the HTTP response
     * @param status    the HTTP status to set
     * @param errorCode the error code to include in the response
     * @param message   the error message to include in the response
     * @throws IOException if an I/O error occurs while writing the response
     */
    // Helper to send standardized error response
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}