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
import me.amlu.authserver.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, HttpServletRequest request, Model model) {

        log.debug("/dashboard called. Authentication object: {}", authentication);
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            log.debug("Principal type: {}", principal.getClass().getName());
            log.debug("Principal value: {}", principal);

            // More specific check for your User entity
            switch (principal) {
                case User appUser -> {
                    log.debug("Principal is an instance of me.amlu.authserver.user.model.User");
                    model.addAttribute("username", appUser.getUsername()); // Should be email

                    model.addAttribute("authorities", appUser.getAuthorities());
                    // You can add more specific attributes from your User entity if needed
                    // model.addAttribute("email", appUser.getEmail().getValue()); // Already covered by getUsername()
                    model.addAttribute("givenName", appUser.getGivenName());
                }
                case UserDetails userDetails -> {
                    log.debug("Principal is an instance of UserDetails (but not your specific User class)");
                    model.addAttribute("username", userDetails.getUsername());
                    model.addAttribute("authorities", userDetails.getAuthorities());
                }
                case OAuth2User oauth2User -> {
                    log.debug("Principal is an instance of OAuth2User");
                    model.addAttribute("username", oauth2User.getAttribute("name"));
                    model.addAttribute("email", oauth2User.getAttribute("email")); // Good to have for OAuth2

                    model.addAttribute("authorities", oauth2User.getAuthorities());
                }
                default -> {
                    log.warn("Principal is of an unexpected type: {}", principal.getClass().getName());
                    // Handle gracefully, perhaps set a generic username
                    model.addAttribute("username", "Authenticated User");
                }
            }
        } else {
            log.warn("/dashboard: No authentication object or principal in SecurityContextHolder!");
            // This case should ideally not be hit if the endpoint is secured and requires authentication.
            // If it is, it might indicate an issue with the security filter chain.
            model.addAttribute("username", "Unknown User");
        }

        // Add csrf token
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            model.addAttribute("csrf", csrf);
        }

        return "pages/dashboard";
    }
}
