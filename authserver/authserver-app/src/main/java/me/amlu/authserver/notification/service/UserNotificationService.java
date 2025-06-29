/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.notification.service;


import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import me.amlu.authserver.notification.config.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service implementation for sending notifications via Slack and Email.
 * This service relies on Spring-managed beans for its clients and configuration.
 */
@Service
public class UserNotificationService {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationService.class);

    private final JavaMailSender emailSender;
    private final MethodsClient slackClient;
    private final NotificationProperties notificationProperties;

    private final Counter emailSuccessCounter;
    private final Counter emailFailureCounter;
    private final Counter slackSuccessCounter;
    private final Counter slackFailureCounter;

    /**
     * Constructs the notification service with its required dependencies.
     *
     * @param emailSender            The pre-configured Spring Mail sender.
     * @param slackClient            The pre-configured Slack API client.
     * @param notificationProperties The application's notification properties.
     * @param meterRegistry          The registry for creating metrics.
     */
    public UserNotificationService(JavaMailSender emailSender, MethodsClient slackClient, NotificationProperties notificationProperties, MeterRegistry meterRegistry) {
        this.emailSender = emailSender;
        this.slackClient = slackClient;
        this.notificationProperties = notificationProperties;

        // Initialize counters using the injected MeterRegistry
        this.emailSuccessCounter = meterRegistry.counter("authserver.notification.email", "status", "success");
        this.emailFailureCounter = meterRegistry.counter("authserver.notification.email", "status", "failure");
        this.slackSuccessCounter = meterRegistry.counter("authserver.notification.slack", "status", "success");
        this.slackFailureCounter = meterRegistry.counter("authserver.notification.slack", "status", "failure");
    }

    /**
     * Sends a notification message to both Slack and a specified email address.
     *
     * @param subject The subject of the notification.
     * @param message The content of the notification.
     * @param email   The recipient's email address.
     */
    @Timed(value = "authserver.notificationservice.sendNotification", description = "Time taken to send a notification for a user")
    public void sendNotification(String subject, String message, String email) {
        // Create a formatted message for Slack that includes the subject
        String slackMessage = String.format("*%s*\n\n%s", subject, message);
        sendSlackNotification(slackMessage);

        // Send email notification using the provided subject
        sendEmailNotification(subject, message, email);
    }

    @Retry(name = "notificationSender")
    private void sendSlackNotification(String message) {
        try {
            // Use the injected client and properties
            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(notificationProperties.getSlackChannel())
                    .text(message)
                    .build();
            // The token is already part of the injected slackClient, no need to add it here.
            slackClient.chatPostMessage(request);
            slackSuccessCounter.increment();
            log.info("Successfully sent notification to Slack channel: {}", notificationProperties.getSlackChannel());
        } catch (SlackApiException | IOException e) {
            slackFailureCounter.increment();
            log.error("Error sending notification to Slack channel '{}': {}", notificationProperties.getSlackChannel(), e.getMessage());
            // Re-throw the exception to trigger the retry mechanism
            throw new RuntimeException("Failed to send Slack notification", e);
        }
    }

    @Retry(name = "notificationSender")
    private void sendEmailNotification(String subject, String text, String toEmail) {
        try {
            // Use the fully configured JavaMailSender bean. It's much simpler.
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(notificationProperties.getSmtpEmailFrom()); // Use property for 'from' address
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(text);

            emailSender.send(mailMessage);
            emailSuccessCounter.increment();
            log.info("Successfully sent email notification to: {}", toEmail);
        } catch (MailException e) {
            emailFailureCounter.increment();
            log.error("Error sending email notification to '{}': {}", toEmail, e.getMessage());
            // Re-throw the exception to trigger the retry mechanism
            throw new RuntimeException("Failed to send email notification", e);
        }
    }
}
