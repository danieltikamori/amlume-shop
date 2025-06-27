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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for handling login and home page requests.
 * This class manages the display of the login form, including error and logout messages,
 * and serves the application's home page.
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Access the login page
 * GET /login
 *
 * // Access the login page with an error message (e.g., from Spring Security)
 * GET /login?error
 * GET /login?error=csrf_token_invalid
 *
 * // Access the login page with a logout message
 * GET /login?logout
 *
 * // Access the home page
 * GET /
 * }</pre>
 *
 * <p><b>Important Notes:</b></p>
 * <ul>
 *     <li>CSRF tokens are typically handled by Spring Security and {@code CsrfTokenAdvice} and do not need manual addition to the model here.</li>
 *     <li>Error messages are tailored based on the {@code errorType} query parameter.</li>
 * </ul>
 */
@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    /**
     * Handles GET requests to the /login endpoint.
     * Displays the login page, potentially with error or logout messages based on query parameters.
     *
     * @param model      The Spring UI model to which attributes are added for the view.
     * @param errorType  An optional query parameter indicating the type of login error.
     *                   Common values might include "error" (for general authentication failure),
     *                   "csrf_token_invalid", "session_expired_logout", or "access_denied".
     * @param logoutFlag An optional query parameter indicating a successful logout.
     * @return The name of the view template to render (e.g., "pages/login").
     */
    @GetMapping("/login")
    public String login(Model model,
                        // Use @RequestParam to get the value of the "error" query parameter
                        @RequestParam(value = "error", required = false) String errorType,
                        // Use @RequestParam for the "logout" query parameter
                        @RequestParam(value = "logout", required = false) String logoutFlag) {
        log.debug("Login page requested. errorType: {}, logoutFlag: {}", errorType, logoutFlag);

        if (errorType != null) {
            model.addAttribute("errorOccurred", true); // A general flag to show an error message
            // Pass the specific error type to the template
            model.addAttribute("errorType", errorType);
            // Set a generic message only if it's a standard form login failure
            // (Spring Security's default failure URL is often /login?error)
            switch (errorType) {
                case "csrf_token_invalid":
                    model.addAttribute("errorMessage", "Security token invalid. Please try again.");
                    break;
                case "session_expired_logout":
                    model.addAttribute("errorMessage", "Your session has expired. Please log in again.");
                    break;
                case "access_denied":
                    model.addAttribute("errorMessage", "Access denied. You do not have permission to view this page.");
                    break;
                case "error": // Default Spring Security error
                default:
                    model.addAttribute("errorMessage", "Invalid username or password.");
                    break;
            }
            log.warn("Login error occurred: {}", errorType);
        }

        if (logoutFlag != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
            log.info("User logged out successfully.");
        }

        // CSRF token is already added by CsrfTokenAdvice, no need to add it manually here
        // if you are using ${csrfHiddenInput} in JTE.
        return "pages/login";
    }

    /**
     * Handles GET requests to the root ("/") endpoint.
     * Displays the application's home page.
     * @return The name of the view template to render (e.g., "pages/home").
     */
    @GetMapping("/")
    public String home() {
        log.debug("Home page requested.");
        return "pages/home";
    }
}
