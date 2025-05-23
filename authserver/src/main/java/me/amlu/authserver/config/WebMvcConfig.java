/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map the /login path directly to the static login-form.html file.
        // The "forward:" prefix tells Spring MVC to perform an internal forward,
        // which will then be handled by the static resource handler.
        registry.addViewController("/login").setViewName("forward:/login-form.html");

        // Map the /status path directly to the static login-status.html file.
        registry.addViewController("/status").setViewName("forward:/login-status.html");

        // Optional: Map the root path "/" to redirect to /login
        // If you want http://localhost:9000/ to automatically go to your login page
        // registry.addViewController("/").setViewName("redirect:/login");
    }
}
