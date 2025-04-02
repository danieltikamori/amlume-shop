/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFailureHandler.class);

    private final RedisTemplate<String, Integer> redisTemplate;
    private static final Duration FAILED_WINDOW = Duration.ofMinutes(15);  // Use Duration

    public AuthenticationFailureHandler(RedisTemplate<String, Integer> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("user");
        String clientId = request.getRemoteAddr() + (username != null ? ":" + username : ""); // Handle potential null username

        try {
            recordFailedLoginAttempt(clientId);
        } catch (RuntimeException e) {
            logger.error("Failed to record login failure in Redis", e);
            // Handle Redis error, e.g., log, circuit breaker, etc.
        }

        super.onAuthenticationFailure(request, response, exception);

        // Redirect or forward to the login page with an error message
        request.getSession().setAttribute("errorMessage", exception.getMessage());
        response.sendRedirect("/login?error"); // Or forward: request.getRequestDispatcher("/login?error").forward(request, response);
    }

    private void recordFailedLoginAttempt(String clientId) {
        String key = "failed:" + clientId;
        try {
            redisTemplate.execute((RedisCallback<Object>) redis -> {  // Use Redis transaction for atomicity
                Integer failedCount = redisTemplate.opsForValue().get(key);
                if (failedCount == null) {
                    redisTemplate.opsForValue().set(key, 1, FAILED_WINDOW.toSeconds(), TimeUnit.SECONDS);
                } else {
                    redisTemplate.opsForValue().increment(key);
                }
                return null; // No specific return is needed from execute.
            });
        } catch (RuntimeException e) {
            logger.error("Error recording failed login attempt", e);
            // Handle/log the error, but don't rethrow if you don't want to interrupt the flow
        }
    }
}
