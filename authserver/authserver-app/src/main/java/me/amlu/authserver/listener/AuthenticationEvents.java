/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.listener;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import me.amlu.authserver.common.IpUtils;
import me.amlu.authserver.common.StringUtils;
import me.amlu.authserver.security.dto.DeviceRegistrationInfo;
import me.amlu.authserver.security.model.GeoLocation;
import me.amlu.authserver.security.service.DeviceFingerprintServiceInterface;
import me.amlu.authserver.security.service.GeoLookupService;
import me.amlu.authserver.user.service.UserServiceInterface;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;

import static me.amlu.authserver.common.HeaderNames.USER_AGENT;
import static me.amlu.authserver.common.IpUtils.IP_HEADERS;

/**
 * Event listener component for handling authentication-related events in the application.
 * This class listens for successful and failed authentication attempts and performs
 * actions such as updating user login statistics, recording device information,
 * and logging geographical location data.
 */
@Component
public class AuthenticationEvents {

    private final UserServiceInterface userService;
    private final DeviceFingerprintServiceInterface deviceFingerprintService;
    private final GeoLookupService geoLookupService;

    /**
     * Constructs an {@code AuthenticationEvents} instance with the necessary service dependencies.
     *
     * @param userService              The service for managing user-related operations, including login handling.
     * @param deviceFingerprintService The service for generating and managing device fingerprints.
     * @param geoLookupService         The service for looking up geographical information based on IP addresses.
     */
    public AuthenticationEvents(UserServiceInterface userService, DeviceFingerprintServiceInterface deviceFingerprintService, GeoLookupService geoLookupService) {
        this.userService = userService;
        this.deviceFingerprintService = deviceFingerprintService;
        this.geoLookupService = geoLookupService;
    }

    /**
     * Handles authentication failure events.
     * This method is triggered when a user fails to authenticate, typically due to bad credentials.
     * It updates the user's failed login attempts count.
     *
     * @param event The {@link AuthenticationFailureBadCredentialsEvent} containing details
     *              about the failed authentication attempt.
     * @usage
     * <pre>
     * // This method is automatically invoked by Spring's event publishing mechanism
     * // when an AuthenticationFailureBadCredentialsEvent is published.
     * </pre>
     */
    @EventListener
    @Timed(value = "authserver.event.authfailure", description = "Time taken to handle auth failure event")
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        if (username != null) {
            userService.handleFailedLogin(username);
        }
    }

    /**
     * Handles authentication success events.
     * This method is triggered upon successful user authentication. It extracts device information
     * (User-Agent, IP address, screen dimensions), performs a geographical lookup, generates a
     * device fingerprint, and then calls the user service to handle the successful login,
     * including recording the device and location details.
     *
     * @param event The {@link AuthenticationSuccessEvent} containing details about the
     *              successful authentication.
     * @see DeviceRegistrationInfo
     */
    @EventListener
    @Timed(value = "authserver.event.authsuccess", description = "Time taken to handle auth success event")
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        // Use Optional to safely extract the username
        Optional<String> usernameOptional = Optional.ofNullable(principal)
                .filter(p -> p instanceof UserDetails || p instanceof String)
                .map(p -> (p instanceof UserDetails userDetails) ? userDetails.getUsername() : (String) p);

        // If a username is present, generate the fingerprint and call the updated service method
        usernameOptional.ifPresent(username -> {
            // Safely get the current HttpServletRequest from the RequestContextHolder
            ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (sra != null) {
                HttpServletRequest request = sra.getRequest();

                // Extract additional device metadata from the request
                String browserInfo = request.getHeader(USER_AGENT);
                String lastKnownIp = getClientIpAddress(request);

                // Use the GeoLookupService to get location details from the IP address
                GeoLocation geoLocation = geoLookupService.lookupLocation(lastKnownIp);

                String deviceName = deviceFingerprintService.getDeviceNameFromUserAgent(browserInfo);
                // Consider making 'source' an enum or a more structured type if multiple sources exist.
                // For now, it's a simple string.
                // Example: DeviceSource.LOGIN_EVENT
                // This helps in maintaining consistency and avoids typos.
                // For this iteration, keeping it as a string as per existing implementation.
                String source = "Login Event";

                // Generate the fingerprint hash
                String fingerprint = deviceFingerprintService.generateDeviceFingerprint(
                        request.getHeader(USER_AGENT),
                        request.getHeader("Screen-Width"),
                        request.getHeader("Screen-Height"),
                        request
                ); // Potentially sensitive headers like Screen-Width/Height should be handled carefully.
                // Ensure these headers are actually sent by the client and are reliable.
                // If not, consider alternative methods for device identification or make them optional.

                // Assemble the DeviceRegistrationInfo DTO
                DeviceRegistrationInfo deviceInfo = DeviceRegistrationInfo.builder()
                        .deviceFingerprint(fingerprint)
                        .browserInfo(browserInfo)
                        .lastKnownIp(lastKnownIp)
                        .location(geoLocation.city())
                        .lastKnownCountry(geoLocation.countryCode())
                        .deviceName(deviceName)
                        .source(source)
                        .build();

                // Call userService.handleSuccessfulLogin with the DTO.
                // This method should be robust enough to handle cases where some device info
                // might be missing or malformed, e.g., if headers are not present.
                // It should also ensure that sensitive data is not logged unnecessarily.
                userService.handleSuccessfulLogin(username, deviceInfo);
            } else {
                // Handle non-web authentication. Pass a DTO representing an unknown device.
                DeviceRegistrationInfo unknownDeviceInfo = DeviceRegistrationInfo.builder()
                        .deviceFingerprint("unknown-non-web-session")
                        .browserInfo("Non-Web")
                        .lastKnownIp("127.0.0.1")
                        .location(GeoLocation.unknown().city())
                        .lastKnownCountry(GeoLocation.unknown().countryCode())
                        .deviceName("System")
                        .source("Non-Web Login")
                        .build();
                userService.handleSuccessfulLogin(username, unknownDeviceInfo);
            }
        });
    }

    /**
     * Extracts the client's IP address from the given {@link HttpServletRequest}.
     * It checks a predefined list of HTTP headers ({@link IpUtils#IP_HEADERS}) in order
     * to find the most reliable IP address, falling back to {@code request.getRemoteAddr()}
     * if no suitable header is found.
     *
     * @param request The {@link HttpServletRequest} from which to extract the IP address.
     * @return The client's IP address as a {@code String}. Returns "127.0.0.1" or similar
     * if the request is local or no external IP can be determined, or the actual
     * remote address.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        return Arrays.stream(IP_HEADERS)
                .map(request::getHeader)
                .filter(ip -> StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip))
                .map(ip -> ip.contains(",") ? ip.split(",")[0].trim() : ip)
                .findFirst()
                .orElseGet(request::getRemoteAddr);
    }
}
