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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.function.Function;

/**
 * Utility methods for authentication operations.
 */
public final class AuthUtils {
    private static final Logger log = LoggerFactory.getLogger(AuthUtils.class);

    private AuthUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Gets the User entity from an Authentication object.
     * This method should be used with a UserRepository to look up the user.
     *
     * @param authentication     The authentication object
     * @param userLookupFunction Function to look up a user by email
     * @param expectedUserType   The Class object representing the expected user type (e.g., User.class)
     * @return The user if found, null otherwise
     */
    public static <T> T getUserFromAuthentication(Authentication authentication,
                                                  Function<String, T> userLookupFunction,
                                                  Class<T> expectedUserType) { // ADDED: Class<T> parameter
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("getUserFromAuthentication: Authentication is null or not authenticated.");
            return null;
        }

        Object principal = authentication.getPrincipal();

        // If principal is already the expected user type
        // Use expectedUserType.isInstance(principal) for type-safe check
        if (expectedUserType.isInstance(principal)) {
            log.debug("Principal is already the expected user type: {}", principal.getClass().getName());
            return expectedUserType.cast(principal); // Type-safe cast
        }

        // Extract email and look up user
        String email = extractEmail(authentication);
        if (email != null) {
            log.debug("Looking up user by email: {}", email);
            return userLookupFunction.apply(email);
        }

        log.warn("Could not determine email from authentication principal");
        return null;
    }

    /**
     * Extracts the primary user identifier (email or username) from an Authentication object.
     * This method should be used by services that then perform a user lookup.
     *
     * @param authentication The authentication object.
     * @return The user's email or username if found, null otherwise.
     */
    public static String getUserIdentifierFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("getUserIdentifierFromAuthentication: Authentication is null or not authenticated.");
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // Assuming username is email
        } else if (principal instanceof OAuth2User oauth2User) {
            // Prioritize 'email' attribute for OAuth2 users
            String email = oauth2User.getAttribute("email");
            if (email != null) {
                return email;
            }
            // Fallback to 'name' attribute if 'email' is not present (e.g., some OAuth2 providers)
            // The 'name' attribute is typically the 'sub' claim or a unique identifier.
            return oauth2User.getName();
        } else if (principal instanceof String strPrincipal) { // Use pattern matching
            return strPrincipal;
        }

        log.warn("Could not determine user identifier from authentication principal of type: {}", principal.getClass().getName());
        return null;
    }

    /**
     * Extracts the email from an Authentication object.
     * Handles different types of authentication principals.
     *
     * @param authentication The authentication object
     * @return The email if found, null otherwise
     */
    public static String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");

            // Fallback logic for OAuth2 providers
            if (email == null && authentication instanceof OAuth2LoginAuthenticationToken oauthToken) {
                String registrationId = oauthToken.getClientRegistration().getRegistrationId();

                // GitHub-specific handling
                if ("github".equalsIgnoreCase(registrationId)) {
                    String login = oauth2User.getAttribute("login");
                    if (login != null) {
                        log.debug("Using GitHub login as identifier: {}", login);
                        return login + "@github.com"; // Fallback format
                    }
                }

                // Google-specific handling
                if ("google".equalsIgnoreCase(registrationId)) {
                    email = oauth2User.getAttribute("email");
                }
            }

            return email;
        } else if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // Assuming username is email
        } else if (principal instanceof String strPrincipal) { // Use pattern matching
            return strPrincipal;
        }

        return null;
    }

    /**
     * Checks if the authentication is from a specific OAuth2 provider.
     *
     * @param authentication The authentication object
     * @param providerName   The provider name (e.g., "google", "github")
     * @return true if the authentication is from the specified provider
     */
    public static boolean isOAuth2Provider(Authentication authentication, String providerName) {
        if (authentication instanceof OAuth2LoginAuthenticationToken oauthToken) {
            String registrationId = oauthToken.getClientRegistration().getRegistrationId();
            return providerName.equalsIgnoreCase(registrationId);
        }
        return false;
    }

    /**
     * Extracts the OAuth2 provider name from an authentication object.
     *
     * @param authentication The authentication object
     * @return The provider name if available, null otherwise
     */
    public static String getOAuth2Provider(Authentication authentication) {
        if (authentication instanceof OAuth2LoginAuthenticationToken oauthToken) {
            return oauthToken.getClientRegistration().getRegistrationId();
        }
        return null;
    }

    /**
     * Checks if the authentication is using OAuth2.
     *
     * @param authentication The authentication object
     * @return true if the authentication is using OAuth2
     */
    public static boolean isOAuth2Authentication(Authentication authentication) {
        return authentication instanceof OAuth2LoginAuthenticationToken;
    }

    /**
     * Extracts the display name from an OAuth2 authentication.
     *
     * @param authentication The authentication object
     * @return The display name if available, null otherwise
     */
    public static String extractOAuth2DisplayName(Authentication authentication) {
        if (!isOAuth2Authentication(authentication)) {
            return null;
        }

        // It's better to pass the OAuth2User directly if it's already extracted,
        // or use pattern matching if it's still the raw principal.
        // Assuming isOAuth2Authentication ensures authentication.getPrincipal() is OAuth2User
        // However, for safety and clarity, let's re-extract with pattern matching.
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oauth2User)) {
            // This case should ideally not happen if isOAuth2Authentication is accurate,
            // but it's a defensive check.
            log.warn("extractOAuth2DisplayName: Principal is not an OAuth2User despite being an OAuth2 authentication.");
            return null;
        }

        // Try common attribute names for display name
        String name = oauth2User.getAttribute("name");
        if (name != null) {
            return name;
        }

        String displayName = oauth2User.getAttribute("display_name");
        if (displayName != null) {
            return displayName;
        }

        // Fallback to provider-specific attributes
        String provider = getOAuth2Provider(authentication);
        if ("github".equalsIgnoreCase(provider)) {
            return oauth2User.getAttribute("login");
        } else if ("google".equalsIgnoreCase(provider)) {
            String givenName = oauth2User.getAttribute("given_name");
            String familyName = oauth2User.getAttribute("family_name");

            if (givenName != null && familyName != null) {
                return givenName + " " + familyName;
            } else if (givenName != null) {
                return givenName;
            }
        }

        return null;
    }
}
