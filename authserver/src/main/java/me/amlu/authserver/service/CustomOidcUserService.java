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
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOidcUserService extends OidcUserService {
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder; // For new users if a password hash is required

    public CustomOidcUserService(UserRepository userRepository, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = (String) attributes.get("email");

        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException("Email not found from OIDC provider");
        }

        Optional<User> userOptional = userRepository.findByEmail_Value(email);
        User localUser = userOptional.orElseGet(() -> {
            User newUser = User.builder()
                    .email(new EmailAddress(email))
                    .firstName((String) attributes.getOrDefault("given_name", email.split("@")[0]))
                    .lastName((String) attributes.get("family_name"))
                    // Password can be null for social-only login or a generated secure random one if your User entity requires it
                    // .password(new HashedPassword(passwordEncoder.encode(UUID.randomUUID().toString())))
                    .build();
            newUser.enableAccount(); // Enable account by default
            authorityRepository.findByAuthority("ROLE_USER").ifPresent(newUser::assignAuthority);
            return userRepository.save(newUser);
        });

        // Update existing user details if necessary (e.g., name changes)
        // localUser.setFirstName((String) attributes.getOrDefault("given_name", localUser.getFirstName()));
        // localUser.setLastName((String) attributes.getOrDefault("family_name", localUser.getLastName()));
        // userRepository.save(localUser);


        return new DefaultOidcUser(localUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
    }
}
    