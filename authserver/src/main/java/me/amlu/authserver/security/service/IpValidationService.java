/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.validation.constraints.NotBlank;
import me.amlu.authserver.exceptions.IpValidationException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class IpValidationService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IpValidationService.class);
    private final LoadingCache<String, Boolean> ipValidationCache;
    private final Pattern ipv4Pattern;

    public IpValidationService() {
        // Compile pattern once during initialization
        this.ipv4Pattern = Pattern.compile(
//            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
                "^((25[0-5]|2[0-4]\\d|\\d{1,2})\\.){3}(25[0-5]|2[0-4]\\d|\\d{1,2})$"
        );

        // Initialize cache
        this.ipValidationCache = CacheBuilder.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build(new CacheLoader<>() {
                    @NullMarked
                    @Override
                    public Boolean load(@NotBlank String ip) {
                        return performIpValidation(ip);
                    }
                });
    }

    public boolean isValidIp(String ip) {
        try {
            return ipValidationCache.get(ip);
        } catch (IpValidationException | ExecutionException e) {
            log.error("Error validating IP: {}", ip, e);
            return false;
        }
    }

    private boolean isValidIpv4(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }

            // Check for leading zeros
//            return !ip.matches(".*\\.[0][0-9]+.*");
//            return !ip.matches(".*\\.\\d+.*"); // Concise regex to check for leading zeros
            return !ip.matches(".*\\.\\d+"); // Even more concise
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidIpv6(String ip) {
        try {
            // Use Java's built-in InetAddress for IPv6 validation
            return InetAddress.getByName(ip) != null;
        } catch (UnknownHostException e) {
            return false;
        }
//        return ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    }

    private boolean performIpValidation(String ip) {
        // Fast regex check first
        if (ipv4Pattern.matcher(ip).matches()) {
            return true;
        }
        // Fall back to more expensive IPv6 validation
        return isValidIpv6(ip);
    }

    // Old implementation

//    private boolean isValidIpAddress(String ip) {
//        if (ip == null || ip.trim().isEmpty()) {
//            return false;
//        }
//
//        // IPv4 validation
//        if (ip.contains(".")) {
//            return isValidIpv4(ip);
//        }
//
//        // IPv6 validation
//        if (ip.contains(":")) {
//            return isValidIpv6(ip);
//        }
//
//        return false;
//    }
//
//    private boolean isValidIpv4(String ip) {
//        try {
//            String[] parts = ip.split("\\.");
//            if (parts.length != 4) {
//                return false;
//            }
//
//            for (String part : parts) {
//                int value = Integer.parseInt(part);
//                if (value < 0 || value > 255) {
//                    return false;
//                }
//            }
//
//            // Check for leading zeros
////            return !ip.matches(".*\\.[0][0-9]+.*");
////            return !ip.matches(".*\\.\\d+.*"); // Concise regex to check for leading zeros
//            return !ip.matches(".*\\.\\d+"); // Even more concise
//        } catch (NumberFormatException e) {
//            return false;
//        }
//    }
//
//    private boolean isValidIpv6(String ip) {
//        try {
//            // Use Java's built-in InetAddress for IPv6 validation
//            return InetAddress.getByName(ip) != null;
//        } catch (UnknownHostException e) {
//            return false;
//        }
////        return ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
//    }
}
