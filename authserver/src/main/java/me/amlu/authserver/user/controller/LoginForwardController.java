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
    import org.springframework.web.bind.annotation.GetMapping;

    /**
     * Controller to forward requests for /login to the static login.html file.
     * This is necessary because Spring Boot's static resource handling might not
     * automatically map /login to /static/login.html without the extension.
     */
    @Controller
    public class LoginForwardController {

        /**
         * Handles GET requests to /login and forwards them to the static login.html file.
         * The 'forward:' prefix tells Spring MVC to perform an internal forward,
         * which will then be handled by the static resource handler.
         *
         * @return A forward directive to the static login.html resource.
         */
        @GetMapping("/login")
        public String forwardLogin() {
            // Forward the request to the static resource path.
            // The static resource handler will then find src/main/resources/static/login.html
            return "forward:/login.html";
        }
    }
