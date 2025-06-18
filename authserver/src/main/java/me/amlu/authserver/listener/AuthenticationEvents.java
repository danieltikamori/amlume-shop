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
import me.amlu.authserver.user.repository.UserRepository;
import me.amlu.authserver.user.service.UserServiceInterface;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class AuthenticationEvents {

    private final UserServiceInterface userService;
    private final UserRepository userRepository;

    public AuthenticationEvents(UserServiceInterface userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @EventListener
    @Timed(value = "authserver.event.authfailure", description = "Time taken to handle auth failure event")
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        if (username != null) {
            userService.handleFailedLogin(username);
        }
    }

    @EventListener
    @Timed(value = "authserver.event.authsuccess", description = "Time taken to handle auth success event")
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        // Use Optional to safely extract the username
        Optional<String> usernameOptional = Optional.ofNullable(principal)
                .filter(p -> p instanceof UserDetails || p instanceof String) // Filter for expected types
                .map(p -> {
                    if (p instanceof UserDetails userDetails) {
                        return userDetails.getUsername();
                    } else { // Must be String, based on filter
                        return (String) p;
                    }
                });

        // The existing logic for handling successful login
        usernameOptional.ifPresent(userService::handleSuccessfulLogin);
    }

//    @EventListener
//    @Timed(value = "authserver.event.authsuccess", description = "Time taken to handle auth success event")
//    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
//        Object principal = event.getAuthentication().getPrincipal();
//        String username = null;
//        if (principal instanceof UserDetails userDetails) { // Apply pattern matching
//            username = userDetails.getUsername();
//        } else if (principal instanceof String strPrincipal) { // Apply pattern matching, use different variable name
//            username = strPrincipal;
//        }
//
//
//        if (username != null) {
//            userService.handleSuccessfulLogin(username);
//        }
//    }
}
