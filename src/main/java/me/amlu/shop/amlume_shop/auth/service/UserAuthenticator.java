/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.auth.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.auth.dto.AuthServerRegistrationRequest;
import me.amlu.shop.amlume_shop.cache_management.service.CacheService;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.security.failedlogin.FailedLoginAttemptService;
import me.amlu.shop.amlume_shop.security.repository.UserDeviceFingerprintRepository;
import me.amlu.shop.amlume_shop.security.service.CaptchaService;
import me.amlu.shop.amlume_shop.security.service.DeviceFingerprintService;
import me.amlu.shop.amlume_shop.security.service.SecurityAuditService;
import me.amlu.shop.amlume_shop.security.service.SecurityNotificationService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserService;
import me.amlu.shop.amlume_shop.user_management.dto.UserRegistrationRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Service
@Transactional
public class UserAuthenticator implements AuthenticationInterface { // Keep interface for now

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UserAuthenticator.class);

    // Keep dependencies for remaining local checks/features
    private final CacheService cacheService; // For local caching (if any remains)
    private final CaptchaService captchaService; // For local captcha validation
    private final MeterRegistry meterRegistry; // For metrics
    private final FailedLoginAttemptService failedLoginAttemptService; // For local IP/username rate limiting
    private final DeviceFingerprintService deviceFingerprintService; // For local device fingerprinting
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository; // For local device fingerprinting storage
    private final HttpServletRequest httpServletRequest; // For request details (IP, User-Agent)
    private final SecurityAuditService auditService; // For local audit logging
    private final SecurityNotificationService notificationService; // For local notifications (e.g., account locked - though this moves to authserver)
    private final UserService userService; // For accessing local User entity (linked by authserver ID)

    // REMOVE dependencies related to old local auth/token flow
    // private final UserRepository userRepository;
    // private final PasswordEncoder passwordEncoder;
    // private final PasetoTokenService pasetoTokenService;
    // private final TokenRevocationService tokenRevocationService;

    // ADD WebClient for calling authserver
    private final WebClient authServerWebClient;

    // Inject maxDevicesPerUser property (if still relevant for amlume-shop's device tracking)
    @Value("${security.max-devices-per-user}")
    private int maxDevicesPerUser;

    // --- Constructor ---
    // Update constructor parameters to reflect remaining dependencies
    public UserAuthenticator(CacheService cacheService, CaptchaService captchaService,
                             MeterRegistry meterRegistry,
                             FailedLoginAttemptService failedLoginAttemptService,
                             DeviceFingerprintService deviceFingerprintService,
                             UserDeviceFingerprintRepository userDeviceFingerprintRepository,
                             HttpServletRequest httpServletRequest,
                             SecurityAuditService auditService,
                             SecurityNotificationService notificationService,
                             UserService userService, // Keep for local user access
                             WebClient authServerWebClient // Inject WebClient
                             // Remove old dependencies: UserRepository, PasswordEncoder, PasetoTokenService, TokenRevocationService
    ) {
        this.cacheService = cacheService;
        this.captchaService = captchaService;
        this.meterRegistry = meterRegistry;
        this.failedLoginAttemptService = failedLoginAttemptService;
        this.deviceFingerprintService = deviceFingerprintService;
        this.userDeviceFingerprintRepository = userDeviceFingerprintRepository;
        this.httpServletRequest = httpServletRequest;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.userService = userService; // Assign local UserService
        this.authServerWebClient = authServerWebClient; // Assign WebClient
    }

    // --- Registration ---
    // Apply Resilience4j annotations
    @Retry(name = "authserverRegistration") // Use the name defined in properties/ResilienceConfig
    @CircuitBreaker(name = "authserverRegistration", fallbackMethod = "registerFallback")
    // Use the name defined in properties/ResilienceConfig
    @Override
    @Transactional // Keep transaction if local pre-flight checks involve DB (e.g., rate limiting)
    @Timed(value = "shopapp.auth.register.call", description = "Time taken for shop-app to call authserver registration")
    public void register(@Valid UserRegistrationRequest request, String ipAddress)
            throws TooManyAttemptsException, InvalidCaptchaException, UserAlreadyExistsException, UserRegistrationException, IllegalArgumentException {

        // Use the mapped userEmail for logging consistency
        String emailForLogging = request.userEmail() != null ? request.userEmail() : "[email_not_provided_in_request]";
        log.info("Attempting to register user via authserver API: userEmail={}", emailForLogging);

        // 0. Perform local pre-flight checks (rate limiting, captcha)
        // Keep these if amlume-shop should enforce them before hitting authserver
        // Use userEmail as the identifier for local rate limiting
        // Ensure request.userEmail() and request.userEmail().getEmail() are not null before calling
        String emailForPreCheck = (request.userEmail() != null)
                ? request.userEmail()
                : "unknown_email_for_precheck"; // Or handle as error
        performPreFlightChecks(emailForPreCheck, ipAddress, "Registration", request.captchaResponse());
        log.debug("Local pre-flight checks passed for registration of user userEmail: {}", emailForPreCheck);

        // 1. Map amlume-shop DTO to authserver DTO
        AuthServerRegistrationRequest authServerRequest = new AuthServerRegistrationRequest(
                request.firstName() != null ? request.firstName() : null,
                request.lastName() != null ? request.lastName() : null,
                request.nickname() != null ? request.nickname() : null,
                request.userEmail() != null ? request.userEmail() : null,
                request.password() != null ? request.password() : null,
                request.mobileNumber() != null ? request.mobileNumber() : null, // Get phone if available
                null  // defaultRegion - assuming not directly available in UserRegistrationRequest, or derive if possible
        );
        // Use the userEmail from the DTO sent to authserver for subsequent logs
        String mappedEmail = authServerRequest.userEmail() != null ? authServerRequest.userEmail() : "[mapped_email_is_null]";
        log.debug("Mapped amlume-shop registration request to authserver DTO for userEmail: {}", mappedEmail);

        // 2. Call authserver's registration API
        // The WebClient call is now wrapped by Resilience4j annotations
        authServerWebClient.post()
                .uri("/api/register") // Endpoint on authserver
                .bodyValue(authServerRequest)
                .retrieve()
                .onStatus(
                        httpStatusCode -> httpStatusCode.is2xxSuccessful(), // Use lambda
                        clientResponse -> {
                            log.info("Authserver registration successful for userEmail: {}", mappedEmail);
                            return Mono.empty();
                        })
                .onStatus(
                        httpStatusCode -> HttpStatus.CONFLICT.equals(httpStatusCode), // Use lambda and equals
                        clientResponse -> {
                            log.warn("Authserver reported user already exists for userEmail: {}", mappedEmail);
                            return Mono.error(new UserAlreadyExistsException("User with this userEmail already exists."));
                        })
                .onStatus(
                        httpStatusCode -> HttpStatus.BAD_REQUEST.equals(httpStatusCode), // Use lambda and equals
                        clientResponse -> {
                            log.warn("Authserver reported bad request for registration of userEmail: {}", mappedEmail);
                            // Explicitly type the Mono chain to Mono<Throwable>
                            Mono<Throwable> errorMono = clientResponse.bodyToMono(String.class)
                                    .flatMap(errorMessage -> {
                                        log.warn("Authserver 400 error details: {}", errorMessage);
                                        // Ensure this returns Mono.error of a Throwable
                                        return Mono.error(new IllegalArgumentException("Invalid registration data: " + errorMessage));
                                    })
                                    .cast(Throwable.class) // Cast the item type to Throwable
                                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid registration data (no details from authserver).")));
                            return errorMono; // Return the explicitly typed Mono<Throwable>
                        })
                .onStatus(
                        HttpStatusCode::isError, // Predicate for any other error
                        clientResponse -> {
                            log.error("Authserver returned error status {} for registration of userEmail: {}", clientResponse.statusCode(), mappedEmail);
                            // Explicitly type the Mono chain to Mono<Throwable>
                            Mono<Throwable> errorMono = clientResponse.bodyToMono(String.class)
                                    .flatMap(errorMessage -> {
                                        log.error("Authserver error details: {}", errorMessage);
                                        // Ensure this returns Mono.error of a Throwable
                                        return Mono.error(new UserRegistrationException("Authserver registration failed: " + errorMessage));
                                    })
                                    .cast(Throwable.class) // Cast the item type to Throwable
                                    .switchIfEmpty(Mono.error(new UserRegistrationException("Authserver registration failed (no details).")));
                            return errorMono; // Return the explicitly typed Mono<Throwable>
                        })
                .bodyToMono(Void.class)
                .block(); // Block to make the method synchronous

        // Audit local registration attempt success (meaning the call to authserver succeeded)
        assert request.userEmail() != null;
        auditService.logSuccessfulRegistration(null, request.userEmail(), ipAddress); // userId is null at this stage in amlume-shop

        log.info("Registration process completed successfully for user: {}", request.userEmail());

        // Note: Specific exceptions like TooManyAttemptsException, InvalidCaptchaException,
        // UserAlreadyExistsException, IllegalArgumentException are handled by the onStatus
        // blocks and re-thrown *before* the generic catch blocks or fallback are triggered
        // for transient/unexpected errors.

    }

    // Fallback method signature must match the original method's parameters + a Throwable
    // It should re-throw specific business exceptions that shouldn't be hidden by resilience,
    // and wrap transient/unexpected errors in a suitable exception for the caller (AuthController).
    public void registerFallback(@Valid UserRegistrationRequest request, String ipAddress, Throwable t)
            throws TooManyAttemptsException, InvalidCaptchaException, UserAlreadyExistsException, UserRegistrationException, IllegalArgumentException {

        String userEmail = request.userEmail() != null ? request.userEmail() : "unknown_email_for_fallback";
        log.error("Fallback for register method triggered for user [{}], IP [{}]. Cause: {}", userEmail, ipAddress, t.getMessage());
        // Audit the failure, potentially with more detail from the exception 't'
        auditService.logFailedRegistration(userEmail, ipAddress, "Authserver registration fallback triggered: " + t.getMessage());

        // Re-throw specific exceptions that were ignored by Retry/CircuitBreaker and should propagate
        // These are typically the exceptions thrown by the onStatus handlers.
        if (t instanceof TooManyAttemptsException) throw (TooManyAttemptsException) t;
        if (t instanceof InvalidCaptchaException) throw (InvalidCaptchaException) t;
        if (t instanceof UserAlreadyExistsException) throw (UserAlreadyExistsException) t;
        if (t instanceof IllegalArgumentException) throw (IllegalArgumentException) t;

        // For other exceptions (network issues, timeouts, circuit breaker open, etc.)
        // throw a generic UserRegistrationException or a more specific fallback exception.
        // The cause 't' provides the original error details.
        if (t instanceof CallNotPermittedException) {
            // Specific message for circuit breaker open
            throw new UserRegistrationException("Registration service is temporarily unavailable (circuit breaker open). Please try again later.", t);
        } else {
            // Generic message for other failures (e.g., after retries exhausted, unexpected WebClient error)
            throw new UserRegistrationException("Registration failed due to an issue communicating with the authentication server. Please try again later.", t);
        }
    }

    // --- Logout ---
    @Transactional
    @Override
    public void logout(String accessToken, String refreshToken) { // Accept tokens to revoke
        // This method needs to be adapted for OAuth2/OIDC logout.
        // It should trigger Spring Security's logout handler which, if configured,
        // will redirect to the authserver's end session endpoint.
        // Token revocation might happen implicitly via the end session endpoint,
        // or you might need to call authserver's token revocation endpoint if it exists.

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            String username = authentication.getName();
            String userId = ""; // Try to get user ID if principal is User object
            // If using OAuth2Login, principal might be OAuth2User or OidcUser
            if (authentication.getPrincipal() instanceof User userPrincipal) { // Assuming local User entity is still used
                userId = String.valueOf(userPrincipal.getUserId());
            } else if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                // Get user ID from claims, e.g., 'sub'
                userId = oauth2User.getName(); // Often 'sub' claim
                // Or get from attributes: userId = String.valueOf(oauth2User.getAttributes().get("sub"));
            }


            log.info("Logging out user: {}", username);

            String reason = "User initiated logout"; // Define a reason for logging

            // Standard Spring Security logout (clears context, invalidates session if applicable)
            // If OidcClientInitiatedLogoutSuccessHandler is configured, this will trigger the redirect to authserver
            new SecurityContextLogoutHandler().logout(httpServletRequest, null, authentication);

            // --- Token Revocation (Optional, depends on authserver's logout flow) ---
            // If authserver's end session endpoint handles token revocation, you might not need this.
            // If you need to explicitly revoke tokens from amlume-shop:
            // You would need to call authserver's token revocation endpoint here using WebClient.
            // This requires amlume-shop to authenticate to the revocation endpoint (e.g., using client credentials).
            // This is more complex than relying on the end session redirect.
            // For now, let's assume the end session redirect is sufficient or token expiry handles it.
            // If you *must* revoke locally issued PASETO tokens (if amlume-shop still issues them *after* OAuth2 login):
            // if (StringUtils.isNotBlank(accessToken)) {
            //     tokenRevocationService.revokeAccessToken(accessToken, TokenConstants.ACCESS_TOKEN_DURATION, reason);
            // }
            // if (StringUtils.isNotBlank(refreshToken)) {
            //     tokenRevocationService.revokeRefreshToken(refreshToken, TokenConstants.REFRESH_TOKEN_DURATION, reason);
            // }
            // --- End Token Revocation ---

            auditService.logLogout(userId, username, httpServletRequest.getRemoteAddr()); // Pass more info to audit
        } else {
            log.debug("Logout called but no authenticated user found in context.");
        }
        // No return value needed typically
    }

    // --- Helper Methods ---

    /**
     * Consolidated pre-flight checks for registration and login.
     * // The 'identifier' parameter will now be an userEmail address.
     * Keep these if amlume-shop should enforce them before hitting authserver/login flow.
     */
    private void performPreFlightChecks(String identifier, String ipAddress, String actionType, String captchaResponse) throws TooManyAttemptsException {

        // Check failed login attempts (applies to log in/MFA verify, maybe registration too)
        // This logic might need adjustment if authserver handles all failed attempts centrally.
        // If amlume-shop still wants to block based on *local* failed attempts (e.g., attempts to hit its login endpoint directly), keep this.
        // If authserver handles it, remove this.
        // For now, assuming local IP/username rate limiting is still desired before calling authserver.
        try {
            failedLoginAttemptService.checkAndThrowIfBlocked(identifier); // Check by username/userEmail
            failedLoginAttemptService.checkAndThrowIfBlocked(ipAddress); // Check by IP
        } catch (TooManyAttemptsException e) {
            // Use the exception message which likely contains the key type information
            auditService.logFailedAttempt(identifier, ipAddress, "Too Many Attempts (Backoff) for " + e.getMessage());
            throw e;
        }

        // Keep Captcha validation here if required for the actionType (e.g., on amlume-shop's registration form)
        if ("Login".equals(actionType) || "Registration".equals(actionType)) {
            captchaService.verifyRateLimitAndCaptcha(captchaResponse, ipAddress);
        }

        log.trace("Local pre-flight checks passed for identifier [{}], action [{}]", identifier, actionType);
    }

    // This method is part of the old PASETO flow and should be removed later.
    @Deprecated(since = "2025-05-15", forRemoval = true)
    private void checkAccountLockStatus(User user, String ipAddress) throws LockedException {
        log.warn("checkAccountLockStatus method is deprecated and should not be used.");
        // Implementation removed
    }

    /**
     * Associates device fingerprint with tokens, checking device limits for new devices.
     * This method is part of the old PASETO flow. If amlume-shop still needs device tracking,
     * this logic needs to be adapted to the OAuth2/OIDC flow (e.g., triggered after OAuth2 login success).
     *
     * @param user              The user.
     * @param accessToken       The access token.
     * @param refreshToken      The refresh token.
     * @param deviceFingerprint The generated device fingerprint.
     * @param trustOnCreate     Whether to mark the device as trusted if it's being created (e.g., during registration).
     * @throws MaxDevicesExceededException if adding a new device would exceed the limit.
     */
    // This method is part of the old PASETO flow. Needs adaptation if local device tracking is kept.
    private void associateFingerprintWithTokens(User user, String accessToken, String refreshToken, String deviceFingerprint, boolean trustOnCreate) throws MaxDevicesExceededException {
        log.warn("associateFingerprintWithTokens method is part of the old PASETO flow. Needs adaptation if local device tracking is kept.");
        // Implementation removed or adapted elsewhere
    }

    @Override
    public String generateAndHandleFingerprint(String userAgent, String screenWidth, String screenHeight) {
        // This method can remain if amlume-shop needs its own device fingerprinting for shop-specific logic
        // (distinct from authserver's passkey/WebAuthn device handling).
        // Its usage would be triggered after successful OAuth2 login.
        log.debug("Generating and handling device fingerprint locally.");
        String deviceFingerprint = deviceFingerprintService.generateDeviceFingerprint(userAgent, screenWidth, screenHeight, httpServletRequest);

        if (deviceFingerprint == null) {
            log.warn("Local device fingerprint could not be generated.");
            return null;
        }
        log.trace("Generated local device fingerprint: {}", deviceFingerprint);
        return deviceFingerprint;
    }

    @Override
    public String determineUserScope() {
        // This method needs to be adapted to read roles/scopes from the JWT claims
        // provided by authserver via the OAuth2 Resource Server configuration.
        // The JwtAuthenticationConverter already maps claims to GrantedAuthority.
        // This method might become redundant if you directly use SecurityContextHolder.getContext().getAuthentication().getAuthorities()
        // where roles are needed.
        log.warn("determineUserScope method needs adaptation to read roles from JWT claims.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            return "ROLE_ANONYMOUS";
        }

        // Assuming authorities are already SimpleGrantedAuthority with "ROLE_" prefix from JwtAuthenticationConverter
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));
    }

}

// TODO: Consider backup codes for account recovery
// TODO: Review transaction boundaries and propagation, especially around cache operations.
// TODO: Add more specific metrics for fingerprinting success/failure/new device detection.
