    /*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

    package me.amlu.authserver.controller;

    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestParam;

    @Controller
    public class LoginViewController {
        private static final Logger log = LoggerFactory.getLogger(LoginViewController.class);

        @GetMapping("/login")
        public String loginPage(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "form_login_success", required = false) String formLoginSuccess,
                                @RequestParam(value = "oauth2_login_success", required = false) String oauth2LoginSuccess) {

            log.info("Serving custom login page. Params: error={}, logout={}, form_login_success={}, oauth2_login_success={}",
                    error, logout, formLoginSuccess, oauth2LoginSuccess);

            if (error != null) {
                model.addAttribute("loginError", "Invalid username or password. Please try again.");
            }
            if (logout != null) {
                model.addAttribute("logoutMessage", "You have been logged out successfully.");
            }
            if (formLoginSuccess != null) {
                model.addAttribute("formLoginSuccess", "Login successful!");
            }
            if (oauth2LoginSuccess != null) {
                model.addAttribute("oauth2LoginSuccess", "OAuth2 login successful!");
            }
            if (error == null && logout == null && formLoginSuccess == null && oauth2LoginSuccess == null) {
                model.addAttribute("statusMessage", "Please log in.");
            }

            return "login-form"; // This refers to src/main/resources/templates/login-form.html
        }
    }
