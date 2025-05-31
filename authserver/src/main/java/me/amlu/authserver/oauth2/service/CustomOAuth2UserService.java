/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.oauth2.service;

import jakarta.annotation.Nullable;
import me.amlu.authserver.oauth2.repository.AuthorityRepository;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.springframework.security.oauth2.core.OAuth2ErrorCodes.SERVER_ERROR;

/**
 * Custom implementation of {@link DefaultOAuth2UserService} to handle the loading and processing
 * of OAuth2 user information from various identity providers.
 * <p>
 * This service is responsible for:
 * <ul>
 *     <li>Fetching user attributes from the OAuth2 provider.</li>
 *     <li>Extracting relevant information like email, first name, last name, and nickname.</li>
 *     <li>Provisioning a new local user if one does not exist for the given email.</li>
 *     <li>Updating existing local user details based on provider attributes.</li>
 *     <li>Ensuring the resulting {@link OAuth2User} contains the correct authorities and attributes
 *         for integration with Spring Security.</li>
 * </ul>
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final WebClient webClient;
    private final AuthorityRepository authorityRepository;


    public CustomOAuth2UserService(UserRepository userRepository,
                                   AuthorityRepository authorityRepository,
                                   WebClient.Builder webClientBuilder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Loads and processes the OAuth2 user information.
     * <p>
     * This method fetches the user attributes from the provider, extracts the necessary details,
     * finds or creates a local user based on the email, updates the local user's profile
     * if necessary, and returns a {@link DefaultOAuth2User} representing the authenticated user
     * with their local authorities and the processed attributes.
     *
     * @param userRequest the user request
     * @return an {@link OAuth2User}
     * @throws OAuth2AuthenticationException if an error occurs during user loading or processing
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info(">>> CustomOAuth2UserService: Attempting to load user for registrationId: {}", userRequest.getClientRegistration().getRegistrationId());
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();
        log.debug("Attributes from provider ({}): {}", userRequest.getClientRegistration().getRegistrationId(), attributes);

        String emailToUse = null;
        User localUser;
//        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails()
//                .getUserInfoEndpoint().getUserNameAttributeName();

        try {
            emailToUse = extractEmail(userRequest, attributes);

            if (!StringUtils.hasText(emailToUse)) {
                log.error("Could not determine a valid email for user from provider '{}'. Attributes: {}", userRequest.getClientRegistration().getRegistrationId(), attributes);
                throw new OAuth2AuthenticationException("Unable to derive email from provider " + userRequest.getClientRegistration().getRegistrationId());
            }

            String firstName = extractFirstName(attributes, userRequest.getClientRegistration().getRegistrationId(), emailToUse);
            String lastName = extractLastName(attributes, userRequest.getClientRegistration().getRegistrationId());
            String nickname = extractNickname(attributes, userRequest.getClientRegistration().getRegistrationId());

            Optional<User> userOptional = userRepository.findByEmail_Value(emailToUse);

            if (userOptional.isEmpty()) {
                log.info("No local user found for email '{}' from provider '{}'. Provisioning new user.", emailToUse, userRequest.getClientRegistration().getRegistrationId());
                User.UserBuilder newUserBuilder = User.builder()
                        .email(new EmailAddress(emailToUse))
                        .firstName(firstName)
                        .lastName(lastName)
                        .nickname(nickname)
                        .externalId(User.generateWebAuthnUserHandle());

                User newUser = newUserBuilder.build();
                newUser.enableAccount();
                authorityRepository.findByAuthority("ROLE_USER").ifPresent(newUser::assignAuthority);
                localUser = userRepository.save(newUser);
                log.info("Successfully provisioned new user ID {} for email '{}' from provider '{}'", localUser.getId(), emailToUse, userRequest.getClientRegistration().getRegistrationId());
            } else {
                localUser = userOptional.get();
                log.debug("Found existing local user ID {} for email '{}' from provider '{}'. Checking for updates.", localUser.getId(), emailToUse, userRequest.getClientRegistration().getRegistrationId());

                boolean updated = false;
                if (StringUtils.hasText(firstName) && !Objects.equals(firstName, localUser.getFirstName())) {
                    localUser.updateFirstName(firstName);
                    updated = true;
                }
                if (!Objects.equals(lastName, localUser.getLastName())) {
                    localUser.updateLastName(lastName);
                    updated = true;
                }
                if (!Objects.equals(nickname, localUser.getNickname())) {
                    localUser.updateNickname(nickname);
                    updated = true;
                }

                if (updated) {
                    localUser = userRepository.save(localUser);
                    log.info("Updated profile details for user ID {} from provider '{}'", localUser.getId(), userRequest.getClientRegistration().getRegistrationId());
                }
            }

            // Ensure attributes map contains the emailToUse, especially if it was fetched separately (like for GitHub)
            // The DefaultOAuth2User will use the 'nameAttributeKey' to determine the principal's name,
            // but other attributes are also carried.
            // It's good practice to ensure the 'email' attribute is consistently the one you want.
            Map<String, Object> finalAttributes = new HashMap<>(attributes); // Create a mutable copy
            finalAttributes.put("email", emailToUse); // Ensure the resolved email is in the attributes

            log.info("<<< CustomOAuth2UserService: Successfully processed user: {}. Returning DefaultOAuth2User with authorities and attributes.", emailToUse);
            return new DefaultOAuth2User(localUser.getAuthorities(), finalAttributes, "email");
//            return new DefaultOAuth2User(localUser.getAuthorities(), finalAttributes, nameAttributeKey);

        } catch (Exception e) {
            log.error("!!! CustomOAuth2UserService: Error during user processing for email '{}', provider '{}': {}", emailToUse, userRequest.getClientRegistration().getRegistrationId(), e.getMessage(), e);
            throw new OAuth2AuthenticationException(new OAuth2Error(SERVER_ERROR), "Error processing OAuth2 user: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the user's email address from the provider attributes.
     * <p>
     * For some providers like GitHub, it may be necessary to make an additional API call
     * to retrieve the user's email address if it's not directly available in the initial attributes.
     *
     * @param userRequest the user request
     * @param attributes  the user attributes provided by the OAuth2 provider
     * @return the extracted email address, or a placeholder if a real email cannot be determined, or null if no email can be derived.
     */
    private String extractEmail(OAuth2UserRequest userRequest, Map<String, Object> attributes) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String emailFromAttributes = (String) attributes.get("email");

        if (StringUtils.hasText(emailFromAttributes)) {
            return emailFromAttributes;
        }

        if ("github".equalsIgnoreCase(registrationId)) {
            log.debug("Email not directly in attributes for GitHub user. Attempting to fetch from /user/emails API.");
            String accessToken = userRequest.getAccessToken().getTokenValue();
            try {
                List<GitHubEmail> emails = webClient.get()
                        .uri("https://api.github.com/user/emails")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<GitHubEmail>>() {
                        })
                        .block(); // Blocking for simplicity in this synchronous service method

                if (emails != null) {
                    Optional<String> primaryVerifiedEmail = emails.stream()
                            .filter(e -> Boolean.TRUE.equals(e.primary()) && Boolean.TRUE.equals(e.verified()))
                            .map(GitHubEmail::email)
                            .findFirst();
                    if (primaryVerifiedEmail.isPresent()) {
                        log.info("Found primary verified email from GitHub API: {}", primaryVerifiedEmail.get());
                        return primaryVerifiedEmail.get();
                    }
                    log.warn("No primary verified email found from GitHub API for user. Attributes: {}", attributes);
                }
            } catch (Exception e) {
                log.error("Failed to fetch emails from GitHub API for user. Attributes: {}. Error: {}", attributes, e.getMessage());
            }
        }

        // Fallback to placeholder if no real email found
        String login = (String) attributes.get("login"); // e.g., GitHub username
        if (StringUtils.hasText(login)) {
            String placeholderEmail = login + "@" + registrationId + ".placeholder.authserver"; // More distinct placeholder
            log.warn("Using placeholder email for provider '{}', login '{}': {}", registrationId, login, placeholderEmail);
            return placeholderEmail;
        }

        return null; // No email could be determined
    }

    /**
     * Extracts the user's first name from the provider attributes.
     * Attempts to use standard OIDC claims first, then common alternatives, and finally
     * falls back to parsing the full name or using the email prefix if necessary.
     * @param attributes The user attributes from the provider.
     * @param registrationId The registration ID of the OAuth2 client.
     * @param email The email address of the user.
     * @return The extracted first name.
     */
    private String extractFirstName(Map<String, Object> attributes, String registrationId, String email) {
        String firstName = (String) attributes.get("given_name"); // Standard OIDC, might not be present for plain OAuth2
        if (!StringUtils.hasText(firstName)) {
            firstName = (String) attributes.get("first_name"); // Common alternative
        }
        return getFirstNameString(attributes, registrationId, email, firstName);
    }

    /**
     * Helper method to derive the first name string.
     * @param attributes The user attributes from the provider.
     * @param registrationId The registration ID of the OAuth2 client.
     * @param email The email address of the user.
     * @param firstName The initially extracted first name.
     * @return The final determined first name string.
     */
    static String getFirstNameString(Map<String, Object> attributes, String registrationId, String email, String firstName) {
        if (!StringUtils.hasText(firstName) && "github".equalsIgnoreCase(registrationId)) {
            String fullName = (String) attributes.get("name");
            if (StringUtils.hasText(fullName)) {
                firstName = fullName.split(" ", 2)[0];
            }
        }

        if (StringUtils.hasText(firstName)) {
            return firstName;
        } else {
            int atIndex = email.indexOf('@');
            return (atIndex != -1) ? email.substring(0, atIndex) : email; // Fallback to full email if no '@'
        }

    }

    /**
     * Extracts the user's last name from the provider attributes.
     * Attempts to use standard OIDC claims first, then common alternatives, and finally
     * falls back to parsing the full name if necessary.
     * @param attributes The user attributes from the provider.
     * @param registrationId The registration ID of the OAuth2 client.
     * @return The extracted last name, or null if not found.
     */
    private String extractLastName(Map<String, Object> attributes, String registrationId) {
        String lastName = (String) attributes.get("family_name");
        if (!StringUtils.hasText(lastName)) {
            lastName = (String) attributes.get("last_name");
        }
        return getLastNameString(attributes, registrationId, lastName);
    }

    /**
     * Helper method to derive the last name string.
     * @param attributes The user attributes from the provider.
     * @param registrationId The registration ID of the OAuth2 client.
     * @param lastName The initially extracted last name.
     * @return The final determined last name string, or null.
     */
    @Nullable
    static String getLastNameString(Map<String, Object> attributes, String registrationId, String lastName) {
        if (!StringUtils.hasText(lastName) && "github".equalsIgnoreCase(registrationId)) {
            String fullName = (String) attributes.get("name");
            if (StringUtils.hasText(fullName)) {
                String[] parts = fullName.split(" ", 2);
                if (parts.length > 1) {
                    lastName = parts[1];
                }
            }
        }
        return lastName;
    }

    /**
     * Extracts the user's nickname from the provider attributes.
     * @param attributes The user attributes from the provider.
     * @param registrationId The registration ID of the OAuth2 client.
     * @return The extracted nickname, or null if not found.
     */
    private String extractNickname(Map<String, Object> attributes, String registrationId) {
        String nickname = (String) attributes.get("nickname");
        if (!StringUtils.hasText(nickname) && "github".equalsIgnoreCase(registrationId)) {
            nickname = (String) attributes.get("login"); // GitHub username
        }
        return nickname;
    }

    /**
     * Simple record to map the response structure from GitHub's /user/emails API endpoint.
     * @param email The email address.
     * @param primary Indicates if this is the primary email.
     * @param verified Indicates if the email address is verified.
     * @param visibility The visibility setting for the email address.
     */
    private record GitHubEmail(String email, Boolean primary, Boolean verified, String visibility) {
    }
}
