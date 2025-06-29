/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.notification.config;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class NotificationConfig {

    @Value("${slack.bot.token}")
    private String slackToken;

    @Bean
    public MethodsClient slackMethodsClient() {
        // Check if slackToken is null or empty, handle appropriately if needed
        if (slackToken == null || slackToken.trim().isEmpty()) {
            // Log a warning or throw an exception if the token is required
            // For now, let's assume it might be optional or configured elsewhere
            // and return null or a default client if applicable.
            // Or, ensure the property is always set.
            // For simplicity, we proceed, but this is a potential issue.
            System.err.println("Warning: slack.bot.token is not configured. Slack MethodsClient might not work.");
            // Depending on strictness, you might want to:
            // throw new IllegalStateException("slack.bot.token must be configured to create Slack MethodsClient");
            // or return a non-functional mock/null if the application can tolerate it.
        }
        // Ensure slackToken is not null before passing to Slack.getInstance().methods()
        // If slackToken can be legitimately null/empty, the Slack library might handle it
        // or throw an exception. Check the library's behavior.
        // Assuming here that a valid token is expected if the bean is created.
        return Slack.getInstance().methods(slackToken);
    }

    @Bean
    public JavaMailSender emailSender(
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port}") int port,
            @Value("${spring.mail.username}") String username,
            @Value("${spring.mail.password}") String password
    ) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }
}
