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

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility methods for IP address operations.
 */
public final class IpUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    private IpUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Gets the client IP address from the request.
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = null;
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }

        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // Handle multiple IPs (X-Forwarded-For can contain a chain)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * Checks if an IP address is valid.
     *
     * @param ip The IP address to check
     * @return true if the IP is valid
     */
    public static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * Checks if an IP address is a local address.
     *
     * @param ip The IP address to check
     * @return true if the IP is a local address
     */
    public static boolean isLocalAddress(String ip) {
        if (!isValidIp(ip)) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Anonymizes an IP address by removing the last octet.
     *
     * @param ip The IP address to anonymize
     * @return The anonymized IP address
     */
    public static String anonymizeIp(String ip) {
        if (!isValidIp(ip)) {
            return ip;
        }

        if (ip.contains(".")) {
            // IPv4
            return ip.substring(0, ip.lastIndexOf(".")) + ".0";
        } else if (ip.contains(":")) {
            // IPv6
            String[] parts = ip.split(":");
            if (parts.length > 4) {
                return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":0:0:0:0";
            }
        }

        return ip;
    }
}
