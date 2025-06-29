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

//import de.codecentric.boot.admin.server.config.EnableAdminServer;
//import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
//import de.codecentric.boot.admin.server.domain.entities.SnapshottingInstanceRepository;
//import de.codecentric.boot.admin.server.eventstore.InstanceEventStore;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
//import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
//
//import de.codecentric.boot.admin.server.config.AdminServerProperties;

//@Configuration
//@EnableAdminServer
public class AdminServerConfig {
//
//    @Bean
//    public InstanceRepository instanceRepository(InstanceEventStore eventStore) {
//        return new SnapshottingInstanceRepository(eventStore);
//    }
//
//    @Bean
//    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, AdminServerProperties adminServerProperties) throws Exception {
//        String adminContextPath = adminServerProperties.getContextPath();
//
//        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
//        successHandler.setTargetUrlParameter("redirectTo");
//        successHandler.setDefaultTargetUrl(adminContextPath + "/");
//
//        http.securityMatcher(adminContextPath + "/**")
//                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
//                        .requestMatchers(adminContextPath + "/assets/**").permitAll()
//                        .requestMatchers(adminContextPath + "/login").permitAll()
//                        .anyRequest().authenticated())
//                .formLogin(formLogin -> formLogin
//                        .loginPage(adminContextPath + "/login")
//                        .successHandler(successHandler))
//                .logout(logout -> logout.logoutUrl(adminContextPath + "/logout"))
//                .httpBasic(Customizer.withDefaults())
//                .csrf(csrf -> csrf
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        .ignoringRequestMatchers(
//                                adminContextPath + "/instances",
//                                adminContextPath + "/instances/*",
//                                adminContextPath + "/actuator/**"
//                        ));
//
//        return http.build();
//    }
}
