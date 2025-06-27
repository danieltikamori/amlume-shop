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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.amlu.authserver.security.audit.SecurityAuditService;
import me.amlu.authserver.security.config.properties.SecurityProperties;
import me.amlu.authserver.exceptions.*;
import me.amlu.authserver.security.dto.DeviceRegistrationInfo;
import me.amlu.authserver.security.model.UserDeviceFingerprint;
import me.amlu.authserver.resilience.ratelimiter.RateLimiter;
import me.amlu.authserver.security.repository.UserDeviceFingerprintRepository;
import me.amlu.authserver.user.dto.UserDeviceResponse;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static me.amlu.authserver.common.Constants.*;
import static me.amlu.authserver.common.ErrorCodes.USER_NOT_FOUND;
import static me.amlu.authserver.common.HeaderNames.USER_AGENT;
import static me.amlu.authserver.common.HeaderNames.X_FORWARDED_FOR;
import static me.amlu.authserver.common.IpUtils.IP_HEADERS;
import static me.amlu.authserver.common.SecurityConstants.HASH_ALGORITHM;
import static me.amlu.authserver.common.SecurityConstants.INITIAL_MAP_CAPACITY;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;

@Service
/**
 * Service implementation for managing device fingerprints.
 * This service handles the registration, validation, deletion, and management
 * of user device fingerprints to enhance security by identifying trusted devices.
 */
@Transactional
public class DeviceFingerprintServiceImpl implements DeviceFingerprintServiceInterface {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintServiceImpl.class);

    private final UserRepository userRepository;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;
    private final SecurityAuditService securityAuditService;

    private final SecurityProperties securityProperties;
    private final IpValidationService ipValidationService;
    private final IpSecurityService ipSecurityService;
    private final RateLimiter rateLimiter;

    @Value("${security.device-fingerprint.fingerprint-salt}")
    private final String fingerprintSalt;

    @Value("${security.max-devices-per-user}")
    private final int maxDevicesPerUser;

    /**
     * Constructs a new DeviceFingerprintServiceImpl.
     *
     * @param fingerprintSalt                 The salt used for hashing device fingerprints, loaded from properties.
     * @param maxDevicesPerUser               The maximum number of devices a user can register, loaded from properties.
     * @param userRepository                  The repository for user data.
     * @param userDeviceFingerprintRepository The repository for user device fingerprint data.
     * @param securityAuditService            The service for logging security-related events.
     * @param securityProperties              The security properties configuration.
     * @param ipValidationService             The service for validating IP addresses.
     * @param ipSecurityService               The service for IP security checks (e.g., blocking, suspicious activity).
     * @param rateLimiter                     The rate limiter for controlling request frequency.
     */
    public DeviceFingerprintServiceImpl(@Value("${security.device-fingerprint.fingerprint-salt}") String fingerprintSalt,
                                        @Value("${security.max-devices-per-user}") int maxDevicesPerUser,
                                        UserRepository userRepository,
                                        UserDeviceFingerprintRepository userDeviceFingerprintRepository, SecurityAuditService securityAuditService,

                                        SecurityProperties securityProperties,
                                        IpValidationService ipValidationService,
                                        IpSecurityService ipSecurityService, @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter
    ) {
        this.fingerprintSalt = Objects.requireNonNull(fingerprintSalt, "Fingerprint salt cannot be null");
        this.maxDevicesPerUser = maxDevicesPerUser;
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.userDeviceFingerprintRepository = Objects.requireNonNull(userDeviceFingerprintRepository, "UserDeviceFingerprintRepository cannot be null");
        this.securityAuditService = securityAuditService;
        this.securityProperties = Objects.requireNonNull(securityProperties, "SecurityProperties cannot be null");
        this.ipValidationService = Objects.requireNonNull(ipValidationService, "IpValidationService cannot be null");
        this.ipSecurityService = Objects.requireNonNull(ipSecurityService, "IpSecurityService cannot be null");
        this.rateLimiter = rateLimiter;
    }

    @Override
    /**
     * Registers a new device fingerprint for a given user.
     * This method performs several security checks including rate limiting, IP validation,
     * and device limit checks before generating and storing the device fingerprint.
     *
     * @param userId The ID of the user for whom to register the device.
     * @param userAgent The User-Agent string from the client's request.
     * @param screenWidth The screen width of the client device.
     * @param screenHeight The screen height of the client device.
     * @param deviceInfo Additional device registration information.
     * @param request The HttpServletRequest containing client headers and IP.
     * @throws DeviceFingerprintRegistrationException If registration fails due to various reasons (e.g., rate limit, max devices, internal error).
     */
    public void registerDeviceFingerprint(String userId, String userAgent, String screenWidth, String screenHeight,
                                          DeviceRegistrationInfo deviceInfo, HttpServletRequest request) throws DeviceFingerprintRegistrationException { // MODIFIED SIGNATURE
        validateInputs(userId, request);
        String clientIp = getClientIpAddress(request); // This is the actual IP from the request

        // --- Apply rate limiting ---
        applyRateLimitingOnRegisterDevice(userId, clientIp);

        // --- Validate IP and spoofing ---
        ipSecurityCheckAtDeviceRegistration(userId, request, clientIp);

        logIpAddressResolution(userId, clientIp, request.getHeader(X_FORWARDED_FOR));

        try {
            User user = findAndValidateUser(userId);

            // Check if user has disabled device fingerprinting
            isDeviceFingerprintingEnabled(user);

            // Check if user has reached maximum devices
            validateDeviceLimit(user);

            // Generate fingerprint
            String fingerprint = generateDeviceFingerprint(userAgent, screenWidth, screenHeight, request);

            // Ensure the deviceInfo contains the generated fingerprint and the clientIp
            DeviceRegistrationInfo finalDeviceInfo = DeviceRegistrationInfo.builder()
                    .deviceFingerprint(fingerprint)
                    .browserInfo(deviceInfo.browserInfo())
                    .lastKnownIp(clientIp) // Use the resolved clientIp
                    .location(deviceInfo.location())
                    .lastKnownCountry(deviceInfo.lastKnownCountry())
                    .deviceName(deviceInfo.deviceName())
                    .source(deviceInfo.source())
                    .build();

            processNewOrExistingFingerprint(user, finalDeviceInfo);
        } catch (Exception e) {
            handleRegistrationError(userId, e);
        }
    }

    /**
     * Performs IP security checks during device registration.
     * This includes checking if the IP is blocked, if it's a valid format, and if there's suspicious activity.
     * @param userId The ID of the user attempting registration.
     * @param request The HttpServletRequest.
     * @param clientIp The resolved client IP address.
     */
    private void ipSecurityCheckAtDeviceRegistration(String userId, HttpServletRequest request, String clientIp) {

        // Check if IP is blocked
        if (ipSecurityService.isIpBlocked(clientIp)) {
            log.warn("Blocked IP address attempted device registration: {}", clientIp);
            securityAuditService.logFailedAttempt(userId, clientIp, "Blocked IP Attempt during device registration");
            throw new BlockedIpAddressException("IP address is blocked");
        }
        // Validate IP
        if (!ipValidationService.isValidIp(clientIp)) {
            log.warn("Invalid IP detected: {}", clientIp);
            securityAuditService.logFailedAttempt(userId, clientIp, "Invalid IP Attempt during device registration");
        }

        // Check for spoofing
        if (ipSecurityService.isIpSuspicious(clientIp, request)) {
            log.warn("Suspicious IP detected: {}", clientIp);
            throw new SecurityException("Suspicious IP activity detected");
        }
    }

    /**
     * Applies rate limiting for device registration attempts based on the client IP.
     * @param userId The ID of the user attempting registration.
     * @param clientIp The client's IP address.
     * @throws DeviceFingerprintRegistrationException If the rate limit is exceeded or the rate limiter is unavailable.
     */
    private void applyRateLimitingOnRegisterDevice(String userId, String clientIp) throws DeviceFingerprintRegistrationException {
        String rateLimitKey = "deviceFingerprintRegister:" + clientIp; // Key by IP for registration attempts
        try {
            if (!rateLimiter.tryAcquire(rateLimitKey)) {
                log.warn("Rate limit exceeded for IP: {} on device registration", clientIp);
                securityAuditService.logFailedAttempt(userId, clientIp, "Rate limit exceeded for device registration");
                throw new DeviceFingerprintRegistrationException("Rate limit exceeded for device registration");
            }
        } catch (RateLimitException e) { // Catch Redis failure if fail-closed
            log.error("Rate limiting check failed for IP: {} on device registration. Denying request.", clientIp, e);
            throw new DeviceFingerprintRegistrationException("Rate limiter unavailable.", e);
        } catch (Exception e) { // Catch other unexpected errors
            log.error("Unexpected error during rate limit check for IP: {} on device registration", clientIp, e);
            throw new DeviceFingerprintRegistrationException("Error during rate limit check.", e);
        }
    }

    /**
     * Validates the input parameters for device registration.
     *
     * @param userId  The user ID.
     * @param request The HttpServletRequest.
     * @throws IllegalArgumentException If userId is blank or request is null.
     */
    private void validateInputs(String userId, HttpServletRequest request) {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        Objects.requireNonNull(request, "Request cannot be null");
    }

    /**
     * Finds a user by ID and validates its existence.
     * @param userId The ID of the user.
     * @return The User entity.
     * @throws UserNotFoundException If the user is not found.
     */
    private User findAndValidateUser(String userId) {
        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
    }

    /**
     * Checks if device fingerprinting is enabled for the given user.
     * @param user The user entity.
     * @throws DeviceFingerprintingDisabledException If device fingerprinting is disabled for the user.
     */
    private void isDeviceFingerprintingEnabled(User user) {
        if (!user.isDeviceFingerprintingEnabled()) {
            log.info("Device fingerprinting is disabled for user: {}", user.getId());
            throw new DeviceFingerprintingDisabledException("Device fingerprinting is disabled");
        }
    }

    /**
     * Validates if the user has reached the maximum allowed number of registered devices.
     * @param user The user entity.
     * @throws MaxDevicesExceededException If the maximum device limit is reached.
     */
    private void validateDeviceLimit(User user) {
        long deviceCount = userDeviceFingerprintRepository.countByUser(user);
        if (deviceCount >= maxDevicesPerUser) {
            log.warn("Maximum device limit reached for user: {}", user.getId());
            throw new MaxDevicesExceededException("Maximum number of devices reached for user");
        }
    }

    /**
     * Decides whether to update an existing device fingerprint's timestamp or create a new device record.
     * If a matching fingerprint exists for the user, its last used timestamp is updated.
     * Otherwise, a new device fingerprint record is created.
     *
     * @param user       The user associated with the device.
     * @param deviceInfo The device registration information.
     */
    private void processNewOrExistingFingerprint(User user, DeviceRegistrationInfo deviceInfo) {
        userDeviceFingerprintRepository
                .findByUserAndDeviceFingerprint(user, deviceInfo.deviceFingerprint())
                .ifPresentOrElse(
                        existing -> updateExistingFingerprint(existing, deviceInfo.lastKnownIp()),
                        () -> createNewFingerprint(user, deviceInfo)
                );
    }

    /**
     * Records that an existing device has been seen (used).
     * This method updates the {@code lastUsedAt} timestamp of the given
     * {@link UserDeviceFingerprint} and saves it to the repository.
     * <p>
     * This private helper simply updates the last used timestamp for a known device.
     */
    private void recordDeviceSeen(UserDeviceFingerprint existing) {
        existing.setLastUsedAt(Instant.now());
        userDeviceFingerprintRepository.save(existing);
        log.debug("Updated last used timestamp for existing device fingerprint: {}",
                existing.getDeviceFingerprint());
    }

    /**
     * Handles errors that occur during device fingerprint registration.
     * Logs the error and re-throws a specific exception based on the original error type.
     * @param userId The ID of the user for whom registration failed.
     * @param e The exception that occurred.
     */
    private void handleRegistrationError(String userId, Exception e) throws DeviceFingerprintRegistrationException {
        log.error("Error registering device fingerprint for user: {}", userId, e);
        if (e instanceof DeviceFingerprintingDisabledException ||
                e instanceof MaxDevicesExceededException) {
            throw new DeviceFingerprintRegistrationException(e.getMessage());
        }
        throw new DeviceFingerprintAdditionException("Failed to register device fingerprint", e);
    }

    @Override
    /**
     * Validates an existing device fingerprint for a user.
     * This method checks the provided fingerprint against the user's registered devices,
     * performs IP security checks, and updates the fingerprint's usage information.
     * @param userId The ID of the user.
     * @param deviceFingerprint The device fingerprint to validate.
     * @param request The HttpServletRequest containing client headers and IP.
     */
    public void validateDeviceFingerprint(String userId, String deviceFingerprint, HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);

        // --- Check rate limiting ---
        rateLimiter.tryAcquire(clientIp);

        try {
            User user = findAndValidateUser(userId);
            UserDeviceFingerprint fingerprint = findAndValidateFingerprint(user, deviceFingerprint);

            validateIpSecurityAtExistingDeviceFingerprintValidation(userId, clientIp, fingerprint, request);
            updateFingerprintUsage(fingerprint, clientIp);

            securityAuditService.logDeviceValidation(user, fingerprint, true);

        } catch (Exception e) {
            handleValidationError(userId, deviceFingerprint, clientIp, e);
        }
    }

    @Override
    /**
     * Deletes a specific device fingerprint for a user.
     * @param userId The ID of the user.
     * @param deviceFingerprint The device fingerprint to delete.
     * @throws DeviceFingerprintDeletionException If deletion fails.
     */
    public void deleteDeviceFingerprint(String userId, String deviceFingerprint) {
        try {
            User user = findAndValidateUser(userId);
            UserDeviceFingerprint fingerprint = findAndValidateFingerprint(user, deviceFingerprint);

            userDeviceFingerprintRepository.delete(fingerprint);
            log.info("Device fingerprint {} deleted for user {}", deviceFingerprint, userId);
            securityAuditService.logDeviceDeletion(user, fingerprint);

        } catch (Exception e) {
            handleDeletionError(userId, deviceFingerprint, e);
        }
    }

    @Override
    /**
     * Deletes a device fingerprint by its ID for a given user.
     * @param user The user entity.
     * @param fingerprintId The ID of the fingerprint to delete.
     */
    public void deleteDeviceFingerprint(User user, Long fingerprintId) {
        try {
            UserDeviceFingerprint fingerprint = userDeviceFingerprintRepository.findById(fingerprintId)
                    .orElseThrow(() -> new DeviceFingerprintNotFoundException("Device Fingerprint not found"));
            userDeviceFingerprintRepository.delete(fingerprint);
            log.info("Device fingerprint {} deleted for user {}", fingerprintId, user.getId());

        } catch (DeviceFingerprintNotFoundException e) {
            log.error("Error deleting fingerprint", e);
            throw e; // Re-throw the exception after logging
        }
    }

    @Override
    /**
     * Generates a unique device fingerprint based on various client-side information.
     * This method collects data such as User-Agent, screen dimensions, browser headers,
     * and IP address, then hashes them with a salt to produce a fingerprint.
     * @param userAgent The User-Agent string.
     * @param screenWidth The screen width.
     * @param screenHeight The screen height.
     * @param request The HttpServletRequest.
     * @return A unique string representing the device fingerprint.
     */
    public String generateDeviceFingerprint(String userAgent, String screenWidth, String screenHeight, HttpServletRequest request) {
        try {
            Map<String, String> data = collectDeviceData(userAgent, screenWidth, screenHeight, request);

            // Browser capabilities and security headers
            addBrowserHeaders(data, request);
            addSecurityHeaders(data, request);

            // Platform-specific information
            addPlatformInfo(data, request);

            if (data.isEmpty()) {
                log.warn("No valid device data collected for fingerprinting");
                return generateFallbackFingerprint();
            }

            return generateFingerprintHash(data);

        } catch (Exception e) {
            log.error("Error generating device fingerprint", e);
            return generateFallbackFingerprint();
        }
    }

    /**
     * Collects various device-related data points from the request and provided parameters.
     * @param userAgent The User-Agent string.
     * @param screenWidth The screen width.
     * @param screenHeight The screen height.
     * @param request The HttpServletRequest.
     * @return A map of collected device data.
     */
    private Map<String, String> collectDeviceData(String userAgent, String screenWidth,
                                                  String screenHeight, HttpServletRequest request) {
        Map<String, String> data = new LinkedHashMap<>(INITIAL_MAP_CAPACITY);

        addDataIfValid(data, USER_AGENT, userAgent);
        addDataIfValid(data, "Screen-Width", screenWidth);
        addDataIfValid(data, "Screen-Height", screenHeight);
        addDataIfValid(data, ACCEPT_LANGUAGE, request.getHeader(ACCEPT_LANGUAGE));
        addDataIfValid(data, "Platform", getPlatform(userAgent));
        addDataIfValid(data, "Time-Zone", getTimeZone(request));
        addDataIfValid(data, "IP-Address", getClientIpAddress(request));
        addSessionData(data, request);

        return data;
    }

    /**
     * Extracts the platform information from the User-Agent string.
     *
     * @param userAgent The User-Agent string.
     * @return A string representing the detected platform (e.g., "Windows", "macOS", "Android").
     */
    private String getPlatform(String userAgent) {
        if (StringUtils.isBlank(userAgent)) {
            return null;
        }

        // Convert to lowercase once and store
        String userAgentLower = userAgent.toLowerCase(Locale.ENGLISH);

        // Use switch for better performance with string matching
        if (userAgentLower.contains("windows")) {
            return "Windows";
        }

        if (userAgentLower.contains("mac")) {  // Simplified Mac detection
            return "macOS";
        }

        if (userAgentLower.contains("linux")) {
            return "Linux";
        }

        if (userAgentLower.contains("android")) {
            return "Android";
        }

        if (userAgentLower.contains("ios")) {
            return "iOS";
        }

        return "Other"; // Or handle unknown platforms differently
    }

    /**
     * Adds standard browser-related HTTP headers to the data map for fingerprinting.
     *
     * @param data    The map to which headers are added.
     * @param request The HttpServletRequest.
     */
    private void addBrowserHeaders(Map<String, String> data, HttpServletRequest request) {
        addDataIfValid(data, "Accept", request.getHeader("Accept"));
        addDataIfValid(data, "Accept-Encoding", request.getHeader("Accept-Encoding"));
        addDataIfValid(data, "Connection", request.getHeader("Connection"));
        addDataIfValid(data, "Host", request.getHeader("Host"));
    }


    /**
     * Adds security-related HTTP headers (e.g., Sec-Fetch-*) to the data map for fingerprinting.
     *
     * @param data    The map to which headers are added.
     * @param request The HttpServletRequest.
     */
    private void addSecurityHeaders(Map<String, String> data, HttpServletRequest request) {
        addDataIfValid(data, "Sec-Fetch-Site", request.getHeader("Sec-Fetch-Site"));
        addDataIfValid(data, "Sec-Fetch-Mode", request.getHeader("Sec-Fetch-Mode"));
        addDataIfValid(data, "Sec-Fetch-User", request.getHeader("Sec-Fetch-User"));
        addDataIfValid(data, "Sec-Fetch-Dest", request.getHeader("Sec-Fetch-Dest"));
        addDataIfValid(data, "Sec-Ch-Ua", request.getHeader("Sec-Ch-Ua"));
        addDataIfValid(data, "Sec-Ch-Ua-Mobile", request.getHeader("Sec-Ch-Ua-Mobile"));
        addDataIfValid(data, "Sec-Ch-Ua-Platform", request.getHeader("Sec-Ch-Ua-Platform"));
    }

    /**
     * Adds custom platform-specific headers (e.g., X-Platform-Type) to the data map for fingerprinting.
     * @param data The map to which headers are added.
     * @param request The HttpServletRequest.
     */
    private void addPlatformInfo(Map<String, String> data, HttpServletRequest request) {
        // Add any custom headers the frontend sends
        addDataIfValid(data, "Platform-Type", request.getHeader("X-Platform-Type"));
        addDataIfValid(data, "App-Version", request.getHeader("X-App-Version"));
        addDataIfValid(data, "Device-Model", request.getHeader("X-Device-Model"));
    }

    /**
     * Adds the HTTP session ID to the data map for fingerprinting, if a session exists.
     * @param data The map to which session data is added.
     * @param request The HttpServletRequest.
     */
    private void addSessionData(Map<String, String> data, HttpServletRequest request) {
        Optional.ofNullable(request.getSession(false))
                .map(HttpSession::getId)
                .ifPresent(sessionId -> addDataIfValid(data, "Session-ID", sessionId));
    }

    /**
     * Generates a cryptographic hash of the collected device data, including a daily rotating timestamp and a salt.
     *
     * @param data The map of device data.
     * @return The hashed device fingerprint.
     * @throws NoSuchAlgorithmException If the specified hash algorithm is not available.
     */
    private String generateFingerprintHash(Map<String, String> data) throws NoSuchAlgorithmException {
        try {
            TreeMap<String, String> sortedData = new TreeMap<>(data);
            StringBuilder rawFingerprint = new StringBuilder(buildRawFingerprint(sortedData));

            // Add timestamp-based component for uniqueness
            String timeComponent = String.valueOf(System.currentTimeMillis() / (24 * 60 * 60 * 1000)); // Daily rotation
            rawFingerprint.append(timeComponent);

            return hashWithSalt(rawFingerprint.toString());
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm not available", e);
            return generateFallbackFingerprint();
        }
    }

    /**
     * Builds a raw string representation of the sorted device data for hashing.
     *
     * @param sortedData A TreeMap containing the sorted device data.
     * @return A string where each key-value pair is joined by ":" and pairs are joined by "|".
     */
    private String buildRawFingerprint(TreeMap<String, String> sortedData) {
        return sortedData.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("|"));
    }

    /**
     * Hashes the input string using the configured hash algorithm and a salt.
     *
     * @param input The string to hash.
     * @return The Base64 URL-encoded hash string.
     * @throws NoSuchAlgorithmException If the hash algorithm is not available.
     */
    private String hashWithSalt(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] saltedInput = (input + fingerprintSalt).getBytes(StandardCharsets.UTF_8);
        byte[] hash = digest.digest(saltedInput);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * Finds and validates a device fingerprint for a given user.
     * Checks if the fingerprint exists and is active.
     * @param user The user entity.
     * @param fingerprintId The ID of the device fingerprint.
     * @return The UserDeviceFingerprint entity.
     * @throws DeviceFingerprintNotFoundException If the fingerprint is not found.
     * @throws InactiveDeviceFingerprintException If the fingerprint is inactive.
     * @throws IllegalArgumentException If user or fingerprintId is invalid.
     */
    private UserDeviceFingerprint findAndValidateFingerprint(User user, String fingerprintId) {
        if (user == null || StringUtils.isBlank(fingerprintId)) {
            throw new IllegalArgumentException("User and fingerprint ID must not be null or empty");
        }

        try {
            UserDeviceFingerprint fingerprint = userDeviceFingerprintRepository
                    .findByUserAndDeviceFingerprint(user, fingerprintId)
                    .orElseThrow(() -> new DeviceFingerprintNotFoundException(
                            String.format("Device Fingerprint not found for user %s", user.getId())));

            if (!fingerprint.isActive()) {
                throw new InactiveDeviceFingerprintException("Device fingerprint is inactive");
            }

            return fingerprint;
        } catch (Exception e) {
            log.error("Error validating fingerprint: {} for user: {}", fingerprintId, user.getId(), e);
            throw new DeviceFingerprintValidationException("Error validating device fingerprint", e);
        }
    }

    /**
     * Performs IP security checks when validating an existing device fingerprint.
     * @param userId The ID of the user.
     * @param clientIp The client's IP address.
     * @param fingerprint The UserDeviceFingerprint being validated.
     * @param request The HttpServletRequest.
     */
    private void validateIpSecurityAtExistingDeviceFingerprintValidation(String userId, String clientIp, UserDeviceFingerprint fingerprint, HttpServletRequest request) {
        try {
            // Check if IP is blocked
            if (ipSecurityService.isIpBlocked(clientIp)) {
                securityAuditService.logFailedAttempt(userId, clientIp, "Blocked IP Attempt during fingerprint validation");
                throw new BlockedIpAddressException("IP address is blocked");
            }

            // Check for spoofing
            if (ipSecurityService.isIpSuspicious(clientIp, request)) {
                throw new SecurityException("Suspicious IP activity detected");
            }

            // Validate IP format
            if (!ipValidationService.isValidIp(clientIp)) {
                securityAuditService.logFailedAttempt(userId, clientIp, "Invalid IP Attempt during fingerprint validation");
                throw new InvalidIpAddressException("Invalid IP address format");
            }

        } catch (RateLimitExceededException | TooManyAttemptsException e) { // Catch the specific exception(s)
            // Handle rate limit exceedance specifically
            securityAuditService.logFailedAttempt(userId, clientIp, "Rate limit exceeded on fingerprint validation");
            // Re-throw the original exception as the calling method might expect it
            throw e;
        } catch (InvalidIpAddressException | BlockedIpAddressException e) {
            // Re-throw specific IP validation exceptions
            throw e;
        } catch (Exception e) {
            // Catch any other unexpected errors
            log.error("Unexpected error during IP security validation for IP {}: {}", clientIp, e.getMessage(), e);
            throw new IpValidationException("Error during IP security validation", e);
        }
    }

    /**
     * Updates the usage details of a device fingerprint, including last used timestamp and IP.
     * @param fingerprint The UserDeviceFingerprint to update.
     * @param clientIp The current client IP address.
     */
    private void updateFingerprintUsage(UserDeviceFingerprint fingerprint, String clientIp) {
        if (fingerprint == null) {
            throw new IllegalArgumentException("Fingerprint cannot be null");
        }

        try {
            Instant now = Instant.now();

            // Update fingerprint details
            fingerprint.setLastUsedAt(now);
            fingerprint.setLastKnownIp(clientIp);

            fingerprint.setUpdateCount(fingerprint.getUpdateCount() + 1);

            // Save and verify the update
            UserDeviceFingerprint updatedFingerprint = userDeviceFingerprintRepository.save(fingerprint);

            if (!updatedFingerprint.getLastUsedAt().equals(now)) {
                throw new DeviceFingerprintUpdateException("Failed to update fingerprint usage");
            }

            // Log the update for audit purposes
            securityAuditService.logFingerprintUpdate(fingerprint.getUser().getId(), clientIp);

        } catch (Exception e) {
            log.error("Error updating fingerprint usage for user: {}",
                    fingerprint.getUser().getId(), e);
            throw new DeviceFingerprintUpdateException("Failed to update fingerprint usage", e);
        }
    }

    /**
     * Resolves the client's IP address from the HttpServletRequest.
     * It checks a predefined list of IP headers (e.g., X-Forwarded-For) before falling back to getRemoteAddr().
     *
     * @param request The HttpServletRequest.
     * @return The resolved client IP address.
     */
    private String getClientIpAddress(HttpServletRequest request) {

        return Arrays.stream(IP_HEADERS)
                .map(request::getHeader)
                .filter(this::isValidIpHeader)
                .map(this::extractFirstIp)
                .findFirst()
                .orElseGet(request::getRemoteAddr);
    }

    /**
     * Checks if an IP header value is valid (not blank and not "unknown").
     *
     * @param ip The IP header value.
     * @return True if valid, false otherwise.
     */
    private boolean isValidIpHeader(String ip) {
        return StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * Extracts the first IP address from a comma-separated string of IP addresses.
     * This is common for headers like X-Forwarded-For.
     * @param ip The string containing one or more IP addresses.
     * @return The first IP address.
     */
    private String extractFirstIp(String ip) {
        return ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    /**
     * Gets the timezone from the request header.
     * If the header is not present or empty, the default timezone of UTC is used.
     *
     * @param request The HTTP request
     * @return The timezone as a string (e.g., "UTC", "America/New_York").
     */
    private String getTimeZone(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Time-Zone"))
                .orElse(ZoneOffset.UTC.getId());
    }

    /**
     * Adds a key-value pair to the data map only if the value is not blank.
     *
     * @param data  The map to add data to.
     * @param key   The key.
     * @param value The value.
     */
    private void addDataIfValid(Map<String, String> data, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            data.put(key, value);
        }
    }

    /**
     * Generates a fallback fingerprint in case the primary fingerprint generation fails.
     * This ensures that a fingerprint is always returned, even if it's not ideal.
     *
     * @return A fallback fingerprint string.
     */
    private String generateFallbackFingerprint() {
        return "fallbackFingerprint_" + UUID.randomUUID();
    }

    /**
     * Handles errors that occur during device fingerprint validation.
     * Logs the error and throws a {@link DeviceFingerprintValidationException}.
     * @param userId The ID of the user.
     * @param fingerprintId The ID of the fingerprint being validated.
     * @param clientIp The client's IP address.
     * @param e The exception that occurred.
     */
    private void handleValidationError(String userId, String fingerprintId, String clientIp, Exception e) {
        log.error("Error validating device fingerprint for user: {} fingerprint: {}", userId, fingerprintId, e);
        securityAuditService.logFailedValidation(userId, fingerprintId, clientIp, e.getMessage());
        throw new DeviceFingerprintValidationException("Failed to validate device fingerprint", e);
    }

    /**
     * Handles errors that occur during device fingerprint deletion.
     * Logs the error and throws a {@link DeviceFingerprintDeletionException}.
     * @param userId The ID of the user.
     * @param fingerprintId The ID of the fingerprint being deleted.
     * @param e The exception that occurred.
     */
    private void handleDeletionError(String userId, String fingerprintId, Exception e) {
        log.error("Error deleting device fingerprint for user: {} fingerprint: {}", userId, fingerprintId, e);
        throw new DeviceFingerprintDeletionException("Failed to delete device fingerprint", e);
    }

    @Override
    public void verifyDeviceFingerprint(String tokenFingerprint, HttpServletRequest request, String userId) throws DeviceFingerprintMismatchException, UserNotFoundException {
        String currentFingerprint = generateDeviceFingerprint(
                request.getHeader("User-Agent"),
                request.getHeader("Screen-Width"),
                request.getHeader("Screen-Height"),
                request);

        // Early validation
        if (tokenFingerprint == null || currentFingerprint == null) {
            throw new DeviceFingerprintMismatchException("Invalid fingerprint data");
        }

        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        // Check if fingerprint matches token
        if (!tokenFingerprint.equals(currentFingerprint)) {
            // Check if it's a known device
            List<UserDeviceFingerprint> fingerprints = userDeviceFingerprintRepository.findByUser(user);

            boolean isKnownDevice = fingerprints != null &&
                    fingerprints.stream().anyMatch(fp -> fp.getDeviceFingerprint().equals(currentFingerprint) && fp.isActive());
            if (!isKnownDevice) {
                throw new DeviceFingerprintMismatchException("Unrecognized device");
            }

            // Update last used timestamp for the matching device
            fingerprints.stream()
                    .filter(fp -> fp.getDeviceFingerprint().equals(currentFingerprint))
                    .findFirst()
                    .ifPresent(this::recordDeviceSeen);
        }

        log.info("Device fingerprint verified for user: {}", userId);
    }

    /**
     * Validates the format of a given device fingerprint string.
     * This method uses a regular expression to ensure the fingerprint adheres to an expected format,
     * typically alphanumeric characters, hyphens, and underscores, within a specified length range.
     *
     * @param fingerprint The device fingerprint string to validate.
     * @return {@code true} if the fingerprint format is valid, {@code false} otherwise.
     */
    private boolean isValidFingerprintFormat(String fingerprint) {
        // Implement the fingerprint validation logic.
        // For example, checking length, allowed characters, etc.
        return fingerprint.matches("^[a-zA-Z0-9-_]{32,256}$");
    }

    /**
     * Marks a specific device as trusted for a user.
     * @param userId The ID of the user.
     * @param deviceFingerprint The fingerprint of the device to trust.
     */
    @Override
    public void trustDevice(long userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .ifPresent(device -> {
                    device.setTrusted(true);
                    device.setLastUsedAt(Instant.now());
                    userDeviceFingerprintRepository.save(device);
                });
    }

    /**
     * Stores a new device fingerprint or updates an existing one for a user.
     * @param user The user entity.
     * @param deviceInfo The device registration information.
     */
    @Override
    public void storeOrUpdateFingerprint(User user, DeviceRegistrationInfo deviceInfo) {
        validateInput(user, deviceInfo.deviceFingerprint()); // Validate using fingerprint from DTO

        userDeviceFingerprintRepository.findByUserAndDeviceFingerprint(user, deviceInfo.deviceFingerprint())
                .ifPresentOrElse(
                        existing -> updateExistingFingerprint(existing, deviceInfo.lastKnownIp()),
                        () -> createNewFingerprint(user, deviceInfo)
                );
    }

    /**
     * Updates an existing device fingerprint's details, such as last used timestamp, failed attempts, and IP.
     * @param fingerprint The UserDeviceFingerprint to update.
     * @param newLastKnownIp The new last known IP address for the device.
     */
    @Override
    public void updateExistingFingerprint(UserDeviceFingerprint fingerprint, String newLastKnownIp) {
        fingerprint.setLastUsedAt(Instant.now());
        fingerprint.setFailedAttempts(0);
        fingerprint.setLastKnownIp(newLastKnownIp); // Update IP on existing fingerprint
        userDeviceFingerprintRepository.save(fingerprint);
        securityAuditService.logDeviceAccess(String.valueOf(fingerprint.getUser().getId()),
                fingerprint.getDeviceFingerprint(),
                "DEVICE_UPDATED", fingerprint.getLastKnownIp());
    }

    /**
     * Creates a new device fingerprint record for a user.
     * This method first checks if the user has reached their maximum device limit.
     * @param user The user entity for whom to create the fingerprint.
     * @param deviceInfo The device registration information.
     * This helper creates a new device record.
     */
    @Override
    public void createNewFingerprint(User user, DeviceRegistrationInfo deviceInfo) {
        checkDeviceLimit(user);

        UserDeviceFingerprint newFingerprint = UserDeviceFingerprint.builder()
                .user(user)
                .deviceFingerprint(deviceInfo.deviceFingerprint())
                .lastUsedAt(Instant.now())
                .active(true)
                .trusted(false)
                .failedAttempts(0)
                .browserInfo(deviceInfo.browserInfo())
                .lastKnownIp(deviceInfo.lastKnownIp())
                .location(deviceInfo.location())
                .lastKnownCountry(deviceInfo.lastKnownCountry())
                .deviceName(deviceInfo.deviceName())
                .source(deviceInfo.source())
                .build();

        userDeviceFingerprintRepository.save(newFingerprint);
        securityAuditService.logDeviceAccess(String.valueOf(user.getId()),
                deviceInfo.deviceFingerprint(),
                "NEW_DEVICE_REGISTERED", deviceInfo.lastKnownIp());
    }

    /**
     * Logs the resolved IP address for a user, including the original header if available, at debug level.
     * @param userId The ID of the user.
     * @param resolvedIp The IP address resolved by the service.
     * @param originalHeader The value of the X-Forwarded-For header, if present.
     */
    private void logIpAddressResolution(String userId, String resolvedIp, String originalHeader) {
        if (log.isDebugEnabled()) {
            log.debug("IP Address resolved for user {}: {} (from header: {})",
                    userId, resolvedIp, originalHeader);
        }
    }

    /**
     * Marks a device as suspicious by incrementing its failed attempts count.
     * If the failed attempts exceed a predefined maximum, the device is deactivated.
     * @param userId The ID of the user.
     * @param deviceFingerprint The fingerprint of the device to mark as suspicious.
     */
    @Override
    public void markDeviceSuspicious(String userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndDeviceFingerprint(Long.valueOf(userId), deviceFingerprint)
                .ifPresent(device -> {
                    device.setFailedAttempts(device.getFailedAttempts() + 1);
                    if (device.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                        device.setActive(false);
                        device.setDeactivatedAt(Instant.now());
                        securityAuditService.logDeviceDeactivation(device.getUser(), device);
                    }
                    userDeviceFingerprintRepository.save(device);
                });
    }

    /**
     * Checks if a user has exceeded the maximum allowed number of registered devices.
     * @param user The user entity.
     * @throws TooManyDevicesException If the device limit is exceeded.
     */
    @Override
    public void checkDeviceLimit(User user) {
        long deviceCount = userDeviceFingerprintRepository.countByUser(user);
        int maxDevices = securityProperties.getMaxDevicesPerUser();

        if (deviceCount >= maxDevices) {
            String errorMessage = String.format(
                    "Maximum device limit (%d) reached for user: %s",
                    maxDevices,
                    user.getId()
            );
            log.warn(errorMessage);
            throw new TooManyDevicesException(errorMessage);
        }
    }

    /**
     * Validates the input for device fingerprint operations.
     * @param user The user entity.
     * @param deviceFingerprint The device fingerprint string.
     * @throws IllegalArgumentException If user is null, fingerprint is null/empty, or format is invalid.
     */
    @Override
    public void validateInput(User user, String deviceFingerprint) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
            throw new IllegalArgumentException("Device fingerprint must not be null or empty");
        }
        if (!isValidFingerprintFormat(deviceFingerprint)) {
            throw new IllegalArgumentException("Invalid fingerprint format");
        }
    }

    /**
     * Verifies if a specific device fingerprint exists and is associated with a user.
     * @param user The user entity.
     * @param fingerprint The device fingerprint string to verify.
     * @return True if the device exists for the user, false otherwise.
     */
    @Override
    public boolean verifyDevice(User user, String fingerprint) {
        return userDeviceFingerprintRepository.existsByUserAndDeviceFingerprint(user, fingerprint);
    }

    /**
     * Revokes all devices for a user, except for a specified fingerprint.
     * @param userId The ID of the user.
     * @param exceptFingerprint The fingerprint of the device to exclude from revocation.
     */
    @Override
    public void revokeAllDevices(String userId, String exceptFingerprint) {
        List<UserDeviceFingerprint> devices = userDeviceFingerprintRepository
                .findByUserIdAndDeviceFingerprintNot(Long.valueOf(userId), exceptFingerprint);

        devices.forEach(device -> {
            device.setActive(false);
            device.setDeactivatedAt(Instant.now());
            // No need to save here, saveAll will do it
        });

        userDeviceFingerprintRepository.saveAll(devices); // Save all changes at once

        securityAuditService.logAllDevicesRevoked(userId, exceptFingerprint);
    }

    /**
     * Deactivates a specific device fingerprint for a user.
     * @param userId The ID of the user.
     * @param deviceFingerprint The fingerprint of the device to deactivate.
     */
    @Override
    public void deactivateDevice(String userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndDeviceFingerprint(Long.valueOf(userId), deviceFingerprint)
                .ifPresent(device -> {
                    device.setActive(false);
                    device.setDeactivatedAt(Instant.now());
                    securityAuditService.logDeviceDeactivation(device.getUser(), device); // Pass User object
                    userDeviceFingerprintRepository.save(device);
                });
    }

    /**
     * Disables device fingerprinting for a user.
     * This also deactivates all active device fingerprints associated with the user.
     * @param user The user entity for whom to disable device fingerprinting.
     */
    @Override
    @Transactional
    public void disableDeviceFingerprinting(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // 1. Create the new disabled state for DeviceFingerprintingInfo
        // Use the existing method on the embeddable which returns a new instance
        me.amlu.authserver.security.model.DeviceFingerprintingInfo currentInfo = user.getDeviceFingerprintingInfo();
        me.amlu.authserver.security.model.DeviceFingerprintingInfo disabledInfo;
        if (currentInfo != null) {
            disabledInfo = currentInfo.disableFingerprinting(); // Creates new instance with enabled=false, fingerprints=null
        } else {
            // Handle case where info might be null (though unlikely with @Embedded)
            disabledInfo = me.amlu.authserver.security.model.DeviceFingerprintingInfo.builder()
                    .deviceFingerprintingEnabled(false)
                    .build();
        }

        // 2. Update the User entity with the new immutable embeddable
        user.updateDeviceFingerprintingInfo(disabledInfo);
        userRepository.save(user); // Save the user with the updated embedded object

        // 3. Find and deactivate associated *active* device fingerprints
        List<UserDeviceFingerprint> activeDevices = userDeviceFingerprintRepository.findByUserAndActiveTrue(user);
        if (!activeDevices.isEmpty()) {
            Instant now = Instant.now();
            for (UserDeviceFingerprint device : activeDevices) {
                device.setActive(false);
                device.setDeactivatedAt(now);
                // Don't save inside the loop for efficiency
            }
            userDeviceFingerprintRepository.saveAll(activeDevices); // Save all changes at once
            log.info("Deactivated {} active device fingerprints for user: {}", activeDevices.size(), user.getId());
        }

        // 4. Logging
        securityAuditService.logDeviceFingerprintingSettingChange(String.valueOf(user.getId()), "DEVICE_FINGERPRINTING_DISABLED", "SUCCESS");
        log.info("Device fingerprinting disabled for user: {}", user.getId()); // Use INFO level
    }

    /**
     * Enables device fingerprinting for a user.
     * This method updates the user's device fingerprinting settings but does not reactivate previously deactivated devices.
     * @param user The user entity for whom to enable device fingerprinting.
     */
    @Override
    @Transactional
    public void enableDeviceFingerprinting(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // 1. Get current info (handle potential null)
        me.amlu.authserver.security.model.DeviceFingerprintingInfo currentInfo = user.getDeviceFingerprintingInfo();
        me.amlu.authserver.security.model.DeviceFingerprintingInfo enabledInfo;

        if (currentInfo != null) {
            // Use the existing method which returns a new instance, preserving existing fingerprints
            enabledInfo = currentInfo.enableFingerprinting();
        } else {
            // If it was somehow null, create a new enabled one from scratch
            enabledInfo = me.amlu.authserver.security.model.DeviceFingerprintingInfo.builder()
                    .deviceFingerprintingEnabled(true)
                    // No existing fingerprints to preserve if currentInfo was null
                    .build();
        }

        // 2. Update the User entity with the new immutable embeddable
        user.updateDeviceFingerprintingInfo(enabledInfo);
        userRepository.save(user); // Save the user with the updated embedded object

        // 3. Logging
        // Note: We don't automatically reactivate devices here. They remain inactive.
        securityAuditService.logDeviceFingerprintingSettingChange(String.valueOf(user.getId()), "DEVICE_FINGERPRINTING_ENABLED", "SUCCESS");
        log.info("Device fingerprinting enabled for user: {}", user.getId());
    }

    /**
     * Lists all registered devices for a given user.
     * @param userId The ID of the user.
     * @param currentDeviceFingerprint The fingerprint of the device making the current request, to mark it as "current".
     * @return A list of {@link UserDeviceResponse} objects.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDeviceResponse> listUserDevices(Long userId, String currentDeviceFingerprint) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        return userDeviceFingerprintRepository.findByUser(user).stream()
                .map(fingerprint -> UserDeviceResponse.builder()
                        .id(fingerprint.getUserDeviceFingerprintId())
                        .deviceName(fingerprint.getDeviceName())
                        .deviceFingerprint(fingerprint.getDeviceFingerprint())
                        .browserInfo(fingerprint.getBrowserInfo())
                        .lastKnownIp(fingerprint.getLastKnownIp())
                        .location(fingerprint.getLocation())
                        .lastKnownCountry(fingerprint.getLastKnownCountry())
                        .lastUsedAt(fingerprint.getLastUsedAt())
                        .active(fingerprint.isActive())
                        .trusted(fingerprint.isTrusted())
                        .currentDevice(fingerprint.getDeviceFingerprint().equals(currentDeviceFingerprint))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Attempts to derive a human-readable device name from a User-Agent string.
     * @param userAgent The User-Agent string from the client.
     * @return A descriptive name for the device (e.g., "Android Device", "Windows PC"), or "Unknown Device".
     */
    @Override
    public String getDeviceNameFromUserAgent(String userAgent) {
        if (StringUtils.isBlank(userAgent)) {
            return "Unknown Device";
        }
        // Simple heuristic: extract OS or browser name
        String lowerCaseUserAgent = userAgent.toLowerCase(Locale.ENGLISH);
        if (lowerCaseUserAgent.contains("android")) return "Android Device";
        if (lowerCaseUserAgent.contains("iphone") || lowerCaseUserAgent.contains("ipad")) return "iOS Device";
        if (lowerCaseUserAgent.contains("windows")) return "Windows PC";
        if (lowerCaseUserAgent.contains("mac")) return "Mac";
        if (lowerCaseUserAgent.contains("linux")) return "Linux PC";
        if (lowerCaseUserAgent.contains("chrome")) return "Chrome Browser";
        if (lowerCaseUserAgent.contains("firefox")) return "Firefox Browser";
        if (lowerCaseUserAgent.contains("safari")) return "Safari Browser";
        if (lowerCaseUserAgent.contains("edge")) return "Edge Browser";
        return "Generic Device";
    }
}
