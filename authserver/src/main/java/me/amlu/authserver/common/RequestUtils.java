/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utility methods for HTTP request operations.
 */
public final class RequestUtils {

    private RequestUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Gets the current HTTP request.
     *
     * @return Optional containing the request if available
     */
    public static Optional<HttpServletRequest> getCurrentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest);
    }

    /**
     * Gets the current HTTP response.
     *
     * @return Optional containing the response if available
     */
    public static Optional<HttpServletResponse> getCurrentResponse() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getResponse);
    }

    /**
     * Gets a cookie value from the request.
     *
     * @param request The HTTP request
     * @param name    The cookie name
     * @return Optional containing the cookie value if found
     */
    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request == null || name == null) {
            return Optional.empty();
        }

        Cookie cookie = WebUtils.getCookie(request, name);
        return Optional.ofNullable(cookie).map(Cookie::getValue);
    }

    /**
     * Adds a cookie to the response.
     *
     * @param response The HTTP response
     * @param name     The cookie name
     * @param value    The cookie value
     * @param maxAge   The cookie max age in seconds
     * @param secure   Whether the cookie is secure
     * @param httpOnly Whether the cookie is HTTP only
     */
    public static void addCookie(HttpServletResponse response, String name, String value,
                                 int maxAge, boolean secure, boolean httpOnly) {
        if (response == null || name == null) {
            return;
        }

        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);
        cookie.setHttpOnly(httpOnly);
        response.addCookie(cookie);
    }

    /**
     * Deletes a cookie from the response.
     *
     * @param response The HTTP response
     * @param name     The cookie name
     */
    public static void deleteCookie(HttpServletResponse response, String name) {
        if (response == null || name == null) {
            return;
        }

        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Gets all request headers as a map.
     *
     * @param request The HTTP request
     * @return Map of header names to values
     */
    public static Map<String, String> getHeadersMap(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        return headers;
    }

    /**
     * Gets the user agent from the request.
     *
     * @param request The HTTP request
     * @return The user agent string
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : null;
    }

    /**
     * Gets the request URL including query string.
     *
     * @param request The HTTP request
     * @return The full request URL
     */
    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        StringBuilder url = new StringBuilder();
        url.append(request.getRequestURL());

        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }

        return url.toString();
    }

    /**
     * Checks if a request is an AJAX request.
     *
     * @param request The HTTP request
     * @return true if the request is an AJAX request
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith);
    }
}
