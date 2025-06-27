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

//import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam; // Import @RequestParam

@Controller
public class LoginController {

//    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginController.class);

    @GetMapping("/login")
    public String login(Model model,
                        // Use @RequestParam to get the value of the "error" query parameter
                        @RequestParam(value = "error", required = false) String errorType,
                        // Use @RequestParam for the "logout" query parameter
                        @RequestParam(value = "logout", required = false) String logoutFlag) {

        if (errorType != null) {
            model.addAttribute("errorOccurred", true); // A general flag to show an error message
            // Pass the specific error type to the template
            model.addAttribute("errorType", errorType);
            // Set a generic message only if it's a standard form login failure
            // (Spring Security's default failure URL is often /login?error)
            if (!"csrf_token_invalid".equals(errorType) &&
                    !"session_expired_logout".equals(errorType) &&
                    !"access_denied".equals(errorType)) {
                model.addAttribute("errorMessage", "Invalid username or password.");
            }
        }

        if (logoutFlag != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }

        // CSRF token is already added by CsrfTokenAdvice, no need to add it manually here
        // if you are using ${csrfHiddenInput} in JTE.

        return "pages/login";
    }

    @GetMapping("/")
    public String home() {
        return "pages/home";
    }
}
