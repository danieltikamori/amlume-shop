/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.amlu.authserver.exceptions.DeviceFingerprintNotFoundException;
import me.amlu.authserver.security.model.UserDeviceFingerprint;
import me.amlu.authserver.security.repository.UserDeviceFingerprintRepository;
import me.amlu.authserver.security.service.DeviceFingerprintServiceInterface;
import me.amlu.authserver.user.dto.UserDeviceResponse;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.service.UserLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for managing user devices.
 * Provides endpoints for viewing and managing devices that have been used to access a user's account.
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceManagementController {

    private static final Logger log = LoggerFactory.getLogger(DeviceManagementController.class);
    private final UserLookupService userLookupService;
    private final DeviceFingerprintServiceInterface deviceFingerprintService;
    private final UserDeviceFingerprintRepository deviceRepository;

    public DeviceManagementController(
            UserLookupService userLookupService,
            DeviceFingerprintServiceInterface deviceFingerprintService,
            UserDeviceFingerprintRepository deviceRepository) {
        this.userLookupService = userLookupService;
        this.deviceFingerprintService = deviceFingerprintService;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Lists all devices associated with the current user's account.
     *
     * @param authentication the current authentication
     * @param request        the HTTP request
     * @return a list of devices
     */
    @GetMapping
    public ResponseEntity<List<UserDeviceResponse>> getUserDevices(
            Authentication authentication,
            HttpServletRequest request) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (currentUser == null) {
            log.warn("Attempt to get devices without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get the current device fingerprint
            String currentFingerprint = deviceFingerprintService.generateDeviceFingerprint(
                    request.getHeader("User-Agent"),
                    request.getHeader("Screen-Width"),
                    request.getHeader("Screen-Height"),
                    request
            );

            // Get all devices for the user
            List<UserDeviceFingerprint> devices = deviceRepository.findByUser(currentUser);

            // Convert to response DTOs
            List<UserDeviceResponse> responseList = devices.stream()
                    .map(device -> UserDeviceResponse.builder()
                            .id(device.getUserDeviceFingerprintId())
                            .deviceName(device.getDeviceName() != null ? device.getDeviceName() : "Unknown Device")
                            .deviceFingerprint(device.getDeviceFingerprint())
                            .browserInfo(device.getBrowserInfo())
                            .lastKnownIp(device.getLastKnownIp())
                            .location(device.getLocation())
                            .lastKnownCountry(device.getLastKnownCountry())
                            .lastUsedAt(device.getLastUsedAt())
                            .active(device.isActive())
                            .trusted(device.isTrusted())
                            .currentDevice(device.getDeviceFingerprint().equals(currentFingerprint))
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.error("Error retrieving devices for user {}: {}", currentUser.getUserId(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve devices", e);
        }
    }

    /**
     * Revokes a specific device, preventing it from being used to access the account.
     *
     * @param authentication the current authentication
     * @param deviceId       the ID of the device to revoke
     * @param request        the HTTP request
     * @return a response with no content
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> revokeDevice(
            Authentication authentication,
            @PathVariable Long deviceId,
            HttpServletRequest request) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (currentUser == null) {
            log.warn("Attempt to revoke device without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get the current device fingerprint
            String currentFingerprint = deviceFingerprintService.generateDeviceFingerprint(
                    request.getHeader("User-Agent"),
                    request.getHeader("Screen-Width"),
                    request.getHeader("Screen-Height"),
                    request
            );

            // Check if trying to revoke current device
            Optional<UserDeviceFingerprint> deviceToRevokeOpt = deviceRepository.findById(deviceId);
            if (deviceToRevokeOpt.isEmpty()) {
                throw new DeviceFingerprintNotFoundException("Device not found");
            }

            UserDeviceFingerprint deviceToRevoke = deviceToRevokeOpt.get();

            // Verify the device belongs to the current user using the repository
            if (!deviceRepository.existsByUser_IdAndId(currentUser.getId(), deviceId)) {
                log.warn("User {} attempted to revoke device {} belonging to another user",
                        currentUser.getId(), deviceId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Prevent revoking current device through this endpoint
            if (deviceToRevoke.getDeviceFingerprint().equals(currentFingerprint)) {
                log.warn("User {} attempted to revoke their current device {}",
                        currentUser.getUserId(), deviceId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .build();
            }

            // Revoke the device
            deviceFingerprintService.deleteDeviceFingerprint(currentUser, deviceId);
            log.info("User {} revoked device {}", currentUser.getUserId(), deviceId);

            return ResponseEntity.noContent().build();
        } catch (DeviceFingerprintNotFoundException e) {
            log.warn("Device not found for ID {}: {}", deviceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found", e);
        } catch (Exception e) {
            log.error("Error revoking device {} for user {}: {}",
                    deviceId, currentUser.getUserId(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not revoke device", e);
        }
    }

    /**
     * Revokes all devices except the current one.
     *
     * @param authentication the current authentication
     * @param request        the HTTP request
     * @return a response with no content
     */
    @DeleteMapping
    public ResponseEntity<Void> revokeAllDevicesExceptCurrent(
            Authentication authentication,
            HttpServletRequest request) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (currentUser == null) {
            log.warn("Attempt to revoke all devices without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get the current device fingerprint
            String currentFingerprint = deviceFingerprintService.generateDeviceFingerprint(
                    request.getHeader("User-Agent"),
                    request.getHeader("Screen-Width"),
                    request.getHeader("Screen-Height"),
                    request
            );

            // Revoke all devices except current
            deviceFingerprintService.revokeAllDevices(currentUser.getUserId().toString(), currentFingerprint);
            log.info("User {} revoked all devices except current", currentUser.getUserId());

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error revoking all devices for user {}: {}",
                    currentUser.getUserId(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not revoke devices", e);
        }
    }

    /**
     * Marks a device as trusted or untrusted.
     *
     * @param authentication the current authentication
     * @param deviceId       the ID of the device
     * @param trusted        whether the device should be trusted
     * @return a response with no content
     */
    @PutMapping("/{deviceId}/trust")
    public ResponseEntity<Void> setDeviceTrusted(
            Authentication authentication,
            @PathVariable Long deviceId,
            @RequestParam boolean trusted) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (currentUser == null) {
            log.warn("Attempt to set device trust without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get the device
            Optional<UserDeviceFingerprint> deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isEmpty()) {
                throw new DeviceFingerprintNotFoundException("Device not found");
            }

            UserDeviceFingerprint device = deviceOpt.get();

            // Verify the device belongs to the current user
            if (!device.getUser().getUserId().equals(currentUser.getUserId())) {
                log.warn("User {} attempted to modify device {} belonging to another user",
                        currentUser.getUserId(), deviceId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Set trust status
            if (trusted) {
                deviceFingerprintService.trustDevice(currentUser.getUserId(), device.getDeviceFingerprint());
                log.info("User {} marked device {} as trusted", currentUser.getUserId(), deviceId);
            } else {
                // If untrustDevice method doesn't exist, we can implement it here
                device.setTrusted(false);
                deviceRepository.save(device);
                log.info("User {} marked device {} as untrusted", currentUser.getUserId(), deviceId);
            }

            return ResponseEntity.noContent().build();
        } catch (DeviceFingerprintNotFoundException e) {
            log.warn("Device not found for ID {}: {}", deviceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found", e);
        } catch (Exception e) {
            log.error("Error setting trust for device {} for user {}: {}",
                    deviceId, currentUser.getUserId(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update device trust status", e);
        }
    }

    /**
     * Toggles device fingerprinting for the current user.
     *
     * @param authentication the current authentication
     * @param enabled        whether device fingerprinting should be enabled
     * @return a response with no content
     */
    @PutMapping("/fingerprinting")
    public ResponseEntity<Void> setDeviceFingerprintingEnabled(
            Authentication authentication,
            @RequestParam boolean enabled) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (currentUser == null) {
            log.warn("Attempt to toggle device fingerprinting without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            if (enabled) {
                deviceFingerprintService.enableDeviceFingerprinting(currentUser);
                log.info("User {} enabled device fingerprinting", currentUser.getUserId());
            } else {
                deviceFingerprintService.disableDeviceFingerprinting(currentUser);
                log.info("User {} disabled device fingerprinting", currentUser.getUserId());
            }

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error setting device fingerprinting for user {}: {}",
                    currentUser.getUserId(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update device fingerprinting settings", e);
        }
    }
}
