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
import me.amlu.authserver.exceptions.DeviceFingerprintMismatchException;
import me.amlu.authserver.exceptions.DeviceFingerprintRegistrationException;
import me.amlu.authserver.exceptions.UserNotFoundException;
import me.amlu.authserver.security.model.UserDeviceFingerprint;
import me.amlu.authserver.user.dto.UserDeviceResponse;
import me.amlu.authserver.user.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service interface for device fingerprinting functionality.
 *
 * <p>Device fingerprinting is a security feature that helps identify and track devices
 * used to access user accounts. It creates a unique identifier for each device based on
 * various attributes like user agent, screen dimensions, IP address, etc.</p>
 *
 * <p>This service provides methods to register, validate, and manage device fingerprints,
 * helping to detect and prevent unauthorized access to user accounts.</p>
 */
public interface DeviceFingerprintService {

    /**
     * Registers a new device fingerprint for a user.
     *
     * @param userId       the ID of the user
     * @param userAgent    the user agent string from the request
     * @param screenWidth  the width of the user's screen
     * @param screenHeight the height of the user's screen
     * @param request      the HTTP request containing additional information
     * @throws DeviceFingerprintRegistrationException if registration fails
     */
    void registerDeviceFingerprint(String userId, String userAgent, String screenWidth, String screenHeight, HttpServletRequest request) throws DeviceFingerprintRegistrationException;

    /**
     * Lists all registered devices for a user.
     *
     * @param userId                   The ID of the user.
     * @param currentDeviceFingerprint The fingerprint of the device making the current request.
     * @return A list of UserDeviceResponse DTOs.
     */
    @Transactional(readOnly = true)
    List<UserDeviceResponse> listUserDevices(Long userId, String currentDeviceFingerprint);


    @Transactional
    void revokeDevice(Long userId, String fingerprintId);

    /**
     * Validates a device fingerprint for a user.
     *
     * @param userId        the ID of the user
     * @param fingerprintId the fingerprint ID to validate
     * @param request       the HTTP request containing additional information
     */
    void validateDeviceFingerprint(String userId, String fingerprintId, HttpServletRequest request);

    /**
     * Deletes a device fingerprint for a user.
     *
     * @param userId            the ID of the user
     * @param deviceFingerprint the fingerprint to delete
     */
    void deleteDeviceFingerprint(String userId, String deviceFingerprint);

    /**
     * Deletes a device fingerprint for a user.
     *
     * @param user          the user
     * @param fingerprintId the ID of the fingerprint to delete
     */
    void deleteDeviceFingerprint(User user, Long fingerprintId);

    /**
     * Generates a device fingerprint based on the provided information.
     *
     * @param userAgent    the user agent string from the request
     * @param screenWidth  the width of the user's screen
     * @param screenHeight the height of the user's screen
     * @param request      the HTTP request containing additional information
     * @return the generated fingerprint
     */
    String generateDeviceFingerprint(String userAgent, String screenWidth, String screenHeight, HttpServletRequest request);

    String generateDeviceFingerprint(HttpServletRequest request);

    /**
     * Verifies that a token's fingerprint matches the current device.
     *
     * @param tokenFingerprint the fingerprint from the token
     * @param request          the HTTP request containing device information
     * @param userId           the ID of the user
     * @throws DeviceFingerprintMismatchException if the fingerprints don't match
     * @throws UserNotFoundException              if the user is not found
     */
    void verifyDeviceFingerprint(String tokenFingerprint, HttpServletRequest request, String userId) throws DeviceFingerprintMismatchException, UserNotFoundException;

    /**
     * Checks if a user has reached the maximum number of registered devices.
     *
     * @param user the user to check
     */
    void checkDeviceLimit(User user);

    /**
     * Validates the input parameters for device fingerprint operations.
     *
     * @param user              the user
     * @param deviceFingerprint the device fingerprint
     */
    void validateInput(User user, String deviceFingerprint);

    /**
     * Verifies if a device is registered for a user.
     *
     * @param user        the user
     * @param fingerprint the fingerprint to verify
     * @return true if the device is registered for the user, false otherwise
     */
    boolean verifyDevice(User user, String fingerprint);

    /**
     * Marks a device as trusted for a user.
     *
     * @param userId      the ID of the user
     * @param fingerprint the fingerprint of the device to trust
     */
    void trustDevice(long userId, String fingerprint);

    /**
     * Extracts a human-readable device name from a User-Agent string.
     *
     * @param userAgent The User-Agent string from the HTTP request.
     * @return A string representing the device name (e.g., "iPhone", "Windows PC").
     */
    String getDeviceNameFromUserAgent(String userAgent);

    /**
     * Stores or updates a device fingerprint for a user.
     *
     * @param user              the user
     * @param deviceFingerprint the device fingerprint
     * @param browserInfo       the browser information
     */
    void storeOrUpdateFingerprint(User user, String deviceFingerprint,
                                  String browserInfo, String lastKnownIp, String location,
                                  String lastKnownCountry, String deviceName, String source);

    /**
     * Updates an existing device fingerprint.
     *
     * @param fingerprint the fingerprint to update
     */
    void updateExistingFingerprint(UserDeviceFingerprint fingerprint);

    /**
     * Creates a new device fingerprint.
     *
     * @param user              the user
     * @param deviceFingerprint the device fingerprint
     * @param browserInfo       the browser information
     * @param lastKnownIp       last used IP address
     * @param location          the location of the device
     * @param lastKnownCountry  last used country
     * @param deviceName        device name set by the user
     * @param source            the source of the fingerprint
     */
    void createNewFingerprint(User user, String deviceFingerprint,
                              String browserInfo, String lastKnownIp, String location,
                              String lastKnownCountry, String deviceName, String source);

    /**
     * Marks a device as suspicious for a user.
     *
     * @param userId      the ID of the user
     * @param fingerprint the fingerprint of the suspicious device
     */
    void markDeviceSuspicious(String userId, String fingerprint);

    /**
     * Revokes all devices for a user except the specified one.
     *
     * @param userId            the ID of the user
     * @param exceptFingerprint the fingerprint of the device to exclude
     */
    void revokeAllDevices(String userId, String exceptFingerprint);

    /**
     * Deactivates a device for a user.
     *
     * @param userId      the ID of the user
     * @param fingerprint the fingerprint of the device to deactivate
     */
    void deactivateDevice(String userId, String fingerprint);

    /**
     * Disables device fingerprinting for a user.
     *
     * @param user the user
     */
    void disableDeviceFingerprinting(User user);

    /**
     * Enables device fingerprinting for a user.
     *
     * @param user the user
     */
    void enableDeviceFingerprinting(User user);

    /**
     * Gets all devices for a user.
     *
     * @param user the user
     * @return a list of device fingerprints
     */
    List<UserDeviceFingerprint> getUserDevices(User user);

    /**
     * Gets a device by its ID.
     *
     * @param deviceId the device ID
     * @return the device fingerprint, or null if not found
     */
    UserDeviceFingerprint getDeviceById(Long deviceId);

    /**
     * Marks a device as untrusted for a user.
     *
     * @param userId      the ID of the user
     * @param fingerprint the fingerprint of the device to untrust
     */
    void untrustDevice(long userId, String fingerprint);
}
