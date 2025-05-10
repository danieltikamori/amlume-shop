/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.service;

import me.amlu.authserver.model.User;
import me.amlu.authserver.model.vo.EmailAddress;
import me.amlu.authserver.repository.AuthorityRepository;
import me.amlu.authserver.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    public CustomOAuth2UserService(UserRepository userRepository, AuthorityRepository authorityRepository) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();
        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        String emailToUse; // This variable will be effectively final

        String emailFromProvider = (String) attributes.get("email");
        if (!StringUtils.hasText(emailFromProvider)) {
            // For some providers like GitHub, email might be null if not public or primary.
            // You might need to make an additional API call to get verified emails.
            // For now, we'll assume email is present or throw.
            // Or, use another unique identifier like 'id' from the provider and store it.
            String login = (String) attributes.get("login"); // e.g., GitHub username
            if (StringUtils.hasText(login)) {
                 emailToUse = login + "@" + userRequest.getClientRegistration().getRegistrationId() + ".example.com"; // Placeholder email
            } else {
                throw new OAuth2AuthenticationException("Email not found from OAuth2 provider, and login/id attribute also missing.");
            }
        } else {
            emailToUse = emailFromProvider;
        }

        // Now, emailToUse holds the definitive email value and is effectively final.

        Optional<User> userOptional = userRepository.findByEmail_Value(emailToUse);
        User localUser = userOptional.orElseGet(() -> {
            // Use emailToUse inside this lambda, as it's effectively final
            String fullName = (String) attributes.get("name");
            String firstName = fullName != null ? fullName.split(" ")[0] : emailToUse.split("@")[0];
            String lastName = fullName != null && fullName.contains(" ") ? fullName.substring(fullName.indexOf(" ") + 1) : null;

            User newUser = User.builder()
                    .email(new EmailAddress(emailToUse)) // Use emailToUse
                    .firstName(firstName)
                    .lastName(lastName)
                    .build();
            newUser.enableAccount(); // Sensible default for new OAuth2 users
            authorityRepository.findByAuthority("ROLE_USER").ifPresent(newUser::assignAuthority);
            return userRepository.save(newUser);
        });

        // Return your User object if it implements UserDetails and OAuth2User,
        // or adapt it to DefaultOAuth2User.
        // The DefaultOAuth2User constructor needs a collection of GrantedAuthority,
        // the attributes map, and the name attribute key.
        return new DefaultOAuth2User(localUser.getAuthorities(), attributes, nameAttributeKey);
    }
}