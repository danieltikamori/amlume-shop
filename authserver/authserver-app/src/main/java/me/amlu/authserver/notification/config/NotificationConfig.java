/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.notification.config;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration class to create beans for notification clients.
 * This class leverages the centralized NotificationProperties for configuration.
 */
@Configuration
public class NotificationConfig {

    /**
     * Creates a singleton Slack MethodsClient, configured with the token
     * from the injected NotificationProperties.
     * <p>
     * The @Validated annotation on NotificationProperties ensures the application
     * will not start if the token is missing, preventing runtime errors.
     *
     * @param properties The application's notification configuration properties.
     * @return A configured MethodsClient instance.
     */
    @Bean
    public MethodsClient slackMethodsClient(NotificationProperties properties) {
        // The @Validated annotation on NotificationProperties ensures the token is not blank.
        return Slack.getInstance().methods(properties.getSlackToken());
    }

    // --- REMOVED JavaMailSender Bean ---
    // Spring Boot's auto-configuration will create and configure this bean for us
    // based on the 'spring.mail.*' properties in application.yml.
    // There is no need to define it manually.

//    @Bean
//    public JavaMailSender emailSender(
//            @Value("${spring.mail.host}") String host,
//            @Value("${spring.mail.port}") int port,
//            @Value("${spring.mail.username}") String username,
//            @Value("${spring.mail.password}") String password
//    ) {
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//        mailSender.setHost(host);
//        mailSender.setPort(port);
//        mailSender.setUsername(username);
//        mailSender.setPassword(password);
//
//        Properties props = mailSender.getJavaMailProperties();
//        props.put("mail.transport.protocol", "smtp");
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//
//        return mailSender;
//    }
}
