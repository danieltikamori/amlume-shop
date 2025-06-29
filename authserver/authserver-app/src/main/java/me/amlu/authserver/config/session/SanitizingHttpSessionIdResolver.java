/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A custom HttpSessionIdResolver that wraps another resolver (by default, the cookie-based one)
 * and sanitizes the resolved session IDs to prevent errors from malformed data,
 * such as null bytes in session cookies.
 */
public class SanitizingHttpSessionIdResolver implements HttpSessionIdResolver {

    private final HttpSessionIdResolver delegate;

    public SanitizingHttpSessionIdResolver(HttpSessionIdResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        // Delegate to the original resolver first
        List<String> unresolvedIds = delegate.resolveSessionIds(request);
        // Sanitize each resolved ID
        return unresolvedIds.stream()
                .map(this::sanitize)
                .collect(Collectors.toList());
    }

    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        // No sanitization needed when setting a valid ID
        delegate.setSessionId(request, response, sessionId);
    }

    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        delegate.expireSession(request, response);
    }

    /**
     * Removes invalid characters from the session ID string.
     * Specifically targets the null character (\0) which causes PostgreSQL errors.
     *
     * @param sessionId The potentially malformed session ID.
     * @return A sanitized session ID, or null if the input was null.
     */
    private String sanitize(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        // Replace any null characters with an empty string
        return sessionId.replace("\0", "");
    }

    /**
     * A static factory method to easily create an instance that wraps the default
     * CookieHttpSessionIdResolver.
     *
     * @return A new SanitizingHttpSessionIdResolver.
     */
    public static HttpSessionIdResolver createDefault() {
        return new SanitizingHttpSessionIdResolver(new CookieHttpSessionIdResolver());
    }
}
