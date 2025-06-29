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

import gg.jte.TemplateEngine;
import gg.jte.springframework.boot.autoconfigure.JteProperties;
import gg.jte.springframework.boot.autoconfigure.JteViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Explicitly configures Spring Web MVC.
 * By using @EnableWebMvc, we take full control, which means we must also
 * re-configure static resource handling.
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    private final TemplateEngine templateEngine;
    private final JteProperties jteProperties;

    /**
     * Constructor to inject both the TemplateEngine and JteProperties.
     * Spring Boot autoconfigures both of these beans from the jte-spring-boot-starter.
     *
     * @param templateEngine The JTE template engine.
     * @param jteProperties  The JTE configuration properties.
     */
    public WebConfig(TemplateEngine templateEngine, JteProperties jteProperties) {
        this.templateEngine = templateEngine;
        this.jteProperties = jteProperties; // ✅ ADDED: Assign injected properties
    }

    /**
     * Configures the JteViewResolver to be the primary view resolver.
     *
     * @return The configured JteViewResolver.
     */
    @Bean
    public ViewResolver jteViewResolver() {
        // Use the correct constructor with both required arguments
        JteViewResolver viewResolver = new JteViewResolver(templateEngine, jteProperties);
        // Ensure this resolver is checked first
        viewResolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return viewResolver;
    }

    /**
     * Re-enables static resource handling, which is disabled by @EnableWebMvc.
     * These paths should match the ones permitted in your SecurityConfig.
     *
     * @param registry The resource handler registry.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/");
        registry.addResourceHandler("/line-awesome/**")
                .addResourceLocations("classpath:/static/line-awesome/");
        registry.addResourceHandler("/favicons/**")
                .addResourceLocations("classpath:/static/favicons/");
        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/static/.well-known/");
    }
}
