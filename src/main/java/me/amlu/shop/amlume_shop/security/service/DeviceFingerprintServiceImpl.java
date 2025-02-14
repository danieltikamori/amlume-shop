/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.model.User;
import me.amlu.shop.amlume_shop.model.UserDeviceFingerprint;
import me.amlu.shop.amlume_shop.repositories.UserDeviceFingerprintRepository;
import me.amlu.shop.amlume_shop.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class DeviceFingerprintServiceImpl implements DeviceFingerprintService {

    private final UserRepository userRepository;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;

    private final String fingerprintSalt;
    @Value("${security.max-devices-per-user}")
    private int maxDevicesPerUser;

    public DeviceFingerprintServiceImpl(@Value("${FINGERPRINT_SALT}") String fingerprintSalt, UserRepository userRepository, UserDeviceFingerprintRepository userDeviceFingerprintRepository) {
        this.fingerprintSalt = fingerprintSalt;
        this.userRepository = userRepository;
        this.userDeviceFingerprintRepository = userDeviceFingerprintRepository;
    }

    // TODO: Salt rotation
    // TODO: Give user choice to disable device fingerprint
    // TODO: Add device fingerprint to user(?)
    @Override
    public void registerDeviceFingerprint(String userId, String userAgent, String screenWidth, String screenHeight, HttpServletRequest request) {
        try {
            String fingerprint = generateDeviceFingerprint(userAgent, screenWidth, screenHeight, request);

            // Check if fingerprint already exists for the user
            User user = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            Optional<UserDeviceFingerprint> existingFingerprint = userDeviceFingerprintRepository.findByUserAndDeviceFingerprint(user, fingerprint);

            if (existingFingerprint.isEmpty()) {
                // Fingerprint does not exist, create a new one
                UserDeviceFingerprint newFingerprint = new UserDeviceFingerprint();
                newFingerprint.setUser(user);
                newFingerprint.setDeviceFingerprint(fingerprint);
                newFingerprint.setCreatedAt(Instant.now());
                newFingerprint.setUpdatedAt(Instant.now());
                userDeviceFingerprintRepository.save(newFingerprint);
            }
        } catch (DeviceFingerprintAdditionException | UserNotFoundException e) {
            log.error("Error adding fingerprint", e);
            throw e; // Re-throw the exception after logging
        }
    }

    @Override
    public void deleteDeviceFingerprint(String userId, Long fingerprintId) {
        try {
            User user = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            deleteDeviceFingerprint(user, fingerprintId);
        } catch (UserNotFoundException e) {
            log.error("Error deleting fingerprint", e);
            throw e; // Re-throw the exception after logging
        }
    }

    @Override
    public void deleteDeviceFingerprint(User user, Long fingerprintId) {
        try {
            UserDeviceFingerprint fingerprint = userDeviceFingerprintRepository.findById(fingerprintId) // Find by ID
                    .orElseThrow(() -> new DeviceFingerprintNotFoundException("Device Fingerprint not found"));

            userDeviceFingerprintRepository.delete(fingerprint); // Delete the entity

        } catch (DeviceFingerprintNotFoundException e) {
            log.error("Error deleting fingerprint", e);
            throw e; // Re-throw the exception after logging
        }
    }
//    @Override
//    public void verifyDeviceFingerprint(String userId, String userAgent, String screenWidth, String screenHeight, HttpServletRequest request) {
//        try {
//            String fingerprint = generateDeviceFingerprint(userAgent, screenWidth, screenHeight, request);
//
//            // Check if fingerprint already exists for the user
//            Optional<UserDeviceFingerprint> existingFingerprint = userDeviceFingerprintRepository.findByUserIdAndFingerprint(Long.valueOf(userId), fingerprint);
//
//            if (existingFingerprint.isEmpty()) {
//                // Fingerprint does not exist, throw exception
//                throw new DeviceFingerprintMismatchException("Device fingerprint mismatch");
//            }
//        } catch (NoSuchAlgorithmException e) {
//            log.error("Error generating fingerprint", e);
//        }
//    }

    @Override
    public String generateDeviceFingerprint(String userAgent, String screenWidth, String screenHeight, HttpServletRequest request) {
        try {
            Map<String, String> data = new TreeMap<>(); // Use TreeMap for automatic sorting

            // Add data points (handle nulls and empty values). Keep alphabetically ordered by key and consistent formatting.
            addData(data, "User-Agent", userAgent);
            addData(data, "Screen-Width", screenWidth);
            addData(data, "Screen-Height", screenHeight);
            addData(data, "Accept-Language", request.getHeader("Accept-Language"));
            addData(data, "Time-Zone", request.getHeader("X-Time-Zone"));

            // Collect network information
            String ipAddress = getClientIpAddress(request);
            String xForwardedFor = request.getHeader("X-Forwarded-For");

//            addData(data, "IP-Address", request.getRemoteAddr());
            addData(data, "IP-Address", ipAddress);
            addData(data, "X-Forwarded-For", xForwardedFor);
            addData(data, "Referer", request.getHeader("Referer"));
            addData(data, "Accept-Encoding", request.getHeader("Accept-Encoding"));
            addData(data, "Accept", request.getHeader("Accept"));
            addData(data, "Connection", request.getHeader("Connection"));
            addData(data, "Host", request.getHeader("Host"));
            addData(data, "Upgrade-Insecure-Requests", request.getHeader("Upgrade-Insecure-Requests"));
            addData(data, "Sec-Fetch-Site", request.getHeader("Sec-Fetch-Site"));
            addData(data, "Sec-Fetch-Mode", request.getHeader("Sec-Fetch-Mode"));
            addData(data, "Sec-Fetch-User", request.getHeader("Sec-Fetch-User"));
            addData(data, "Sec-Fetch-Dest", request.getHeader("Sec-Fetch-Dest"));
            addData(data, "Sec-Ch-Ua", request.getHeader("Sec-Ch-Ua"));
            addData(data, "Sec-Ch-Ua-Mobile", request.getHeader("Sec-Ch-Ua-Mobile"));
            addData(data, "Sec-Ch-Ua-Platform", request.getHeader("Sec-Ch-Ua-Platform"));


            // Check if data is empty
            if (data.isEmpty()) {
//                return UUID.randomUUID().toString(); // Fallback if no data
                return null; // Or handle as appropriate
            }

            // Normalize and order data
            StringBuilder rawFingerprint = new StringBuilder();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                rawFingerprint.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }

            // Add salt and hash
            String saltedFingerprint = rawFingerprint.toString() + fingerprintSalt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(saltedFingerprint.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating fingerprint", e);
//            return UUID.randomUUID().toString(); // Fallback if SHA-256 is not available
            return null; // Or handle as appropriate
        }
    }

    private void addData(Map<String, String> data, String key, String value) {
        if (value != null && !value.isEmpty()) {
            data.put(key, value);
        }
    }

    @Override
    public void verifyDeviceFingerprint(String tokenFingerprint, HttpServletRequest request, String userId) throws DeviceFingerprintMismatchException, UserNotFoundException {
        String currentFingerprint = generateDeviceFingerprint(request.getHeader("User-Agent"), request.getHeader("Screen-Width"), request.getHeader("Screen-Height"), request);

        if (tokenFingerprint == null || currentFingerprint == null || !tokenFingerprint.equals(currentFingerprint)) {
            User user = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            List<UserDeviceFingerprint> fingerprints = userDeviceFingerprintRepository.findByUser(user);

            if (fingerprints == null || fingerprints.stream().noneMatch(fp -> fp.getDeviceFingerprint().equals(currentFingerprint))) {
                log.warn("Device fingerprint mismatch for user: {}", userId);
                throw new DeviceFingerprintMismatchException("Device fingerprint mismatch");
            } else {
                log.info("Device fingerprint matched for user: {}", userId);
            }
        }
    }

    @Override
    public boolean verifyDevice(User user, String fingerprint) {
        return userDeviceFingerprintRepository.existsByUserAndFingerprint(user, fingerprint);
    }

    private void validateInput(User user, String deviceFingerprint) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
            throw new IllegalArgumentException("Device fingerprint must not be null or empty");
        }
        // Add fingerprint format validation if needed
        if (!isValidFingerprintFormat(deviceFingerprint)) {
            throw new IllegalArgumentException("Invalid fingerprint format");
        }
    }

    private boolean isValidFingerprintFormat(String fingerprint) {
        // Implement your fingerprint validation logic
        // For example, checking length, allowed characters, etc.
        return fingerprint.matches("^[a-zA-Z0-9-_]{32,256}$");
    }

    @Override
    public void trustDevice(long userId, String fingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndFingerprint(userId, fingerprint)
                .ifPresent(device -> {
                    device.setTrusted(true);
                    device.setLastUsedAt(Instant.now());
                    userDeviceFingerprintRepository.save(device);
                });
    }


    @Override
    public void storeOrUpdateFingerprint(User user, String accessToken, String refreshToken, String deviceFingerprint) {
        // Input validation
        if (user == null || deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
            throw new IllegalArgumentException("User and device fingerprint must not be null or empty");
        }

        userDeviceFingerprintRepository.findByUserAndDeviceFingerprint(user, deviceFingerprint)
                .ifPresentOrElse(
                        this::updateExistingFingerprint,
                        () -> createNewFingerprint(user, accessToken, refreshToken, deviceFingerprint)
                );
    }

    private void updateExistingFingerprint(UserDeviceFingerprint fingerprint) {
        fingerprint.setLastUsedAt(Instant.now());
        userDeviceFingerprintRepository.save(fingerprint);
        auditLogger.logDeviceAccess(fingerprint.getUser().getUserId(),
                fingerprint.getDeviceFingerprint(),
                "DEVICE_ACCESS");
    }

    private void createNewFingerprint(User user, String accessToken, String refreshToken, String deviceFingerprint) {
        long deviceCount = userDeviceFingerprintRepository.countByUser(user);
        if (deviceCount >= 5) {
            log.warn("Too many devices registered for user: {}", user.getUserId());
            throw new TooManyDevicesException("Maximum device limit (5) reached. Please remove a device.");
        }

        UserDeviceFingerprint newFingerprint = new UserDeviceFingerprint();
        newFingerprint.setUser(user);
        newFingerprint.setDeviceFingerprint(deviceFingerprint);
        newFingerprint.setCreatedAt(Instant.now());
        newFingerprint.setUpdatedAt(Instant.now());
        newFingerprint.setLastUsedAt(Instant.now());
        newFingerprint.setAccessToken(accessToken);
        newFingerprint.setRefreshToken(refreshToken);
        userDeviceFingerprintRepository.save(newFingerprint);
    }

    private void createNewFingerprint(User user, String deviceFingerprint) {
        checkDeviceLimit(user);

        UserDeviceFingerprint newFingerprint = UserDeviceFingerprint.builder()
                .user(user)
                .deviceFingerprint(deviceFingerprint)
                .lastUsedAt(Instant.now())
                .build();

        userDeviceFingerprintRepository.save(newFingerprint);
        auditLogger.logDeviceAccess(user.getUserId(),
                deviceFingerprint,
                "NEW_DEVICE_REGISTERED");
    }

    private void checkDeviceLimit(User user) {
        long deviceCount = userDeviceFingerprintRepository.countByUser(user);
        if (deviceCount >= securityProperties.getMaxDevicesPerUser()) {
            String errorMessage = String.format(
                    "Maximum device limit (%d) reached for user: %s",
                    securityProperties.getMaxDevicesPerUser(),
                    user.getUserId()
            );
            log.warn(errorMessage);
            throw new TooManyDevicesException(errorMessage);
        }
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating device fingerprint hash", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] HEADERS_TO_TRY = {
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

        for (String header : HEADERS_TO_TRY) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
}