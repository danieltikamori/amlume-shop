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

import com.mailersend.sdk.Recipient;
import com.nimbusds.jose.JWEObjectJSON;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import io.github.resilience4j.retry.annotation.Retry;
import io.vavr.API;
import me.amlu.authserver.notification.config.NotificationProperties;
import org.apache.kafka.common.utils.Java;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.exceptions.MailerSendException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for sending notifications via Slack and Email.
 * This service relies on Spring-managed beans for its clients and configuration.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final MethodsClient slackClient;
    private final JavaMailSender emailSender;
    private final NotificationProperties notificationProperties;

    /**
     * Constructs the notification service with its required dependencies.
     *
     * @param emailSender            The pre-configured Spring Mail sender.
     * @param slackClient            The pre-configured Slack API client.
     * @param notificationProperties The application's notification properties.
     */
    public NotificationServiceImpl(JavaMailSender emailSender, MethodsClient slackClient, NotificationProperties notificationProperties) {
        this.emailSender = emailSender;
        this.slackClient = slackClient;
        this.notificationProperties = notificationProperties;
    }

    /**
     * Sends a notification message to both Slack and a specified email address.
     *
     * @param message The content of the notification.
     * @param email   The recipient's email address.
     */
    @Override
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
            log.info("Successfully sent notification to Slack channel: {}", notificationProperties.getSlackChannel());
        } catch (SlackApiException | IOException e) {
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
            log.info("Successfully sent email notification to: {}", toEmail);
        } catch (MailException e) {
            log.error("Error sending email notification to '{}': {}", toEmail, e.getMessage());
            // Re-throw the exception to trigger the retry mechanism
            throw new RuntimeException("Failed to send email notification", e);
        }
    }


    public void sendEmail() {

        Email email = new Email();

        email.setFrom("name", "your email");
        email.addRecipient("name", "your@recipient.com");

        // you can also add multiple recipients by calling addRecipient again
        email.addRecipient("name 2", "your@recipient2.com");

        // there's also a recipient object you can use
        Recipient recipient = new Recipient("name", "your@recipient3.com");
        email.AddRecipient(recipient);

        Recipient ccRecipient = new Recipient("name", "your@recipient3.com");


//        List<Recipient> ccRecipient =
//        email.cc(ccRecipient);
//
//        // add a second cc recipient
//        email.cc("name 2", "yourCc2@recipient.com");
//
//        // same for a bcc recipient
//        email.bcc("bcc name", "yourBcc@recipient.com");

        email.setSubject("Email subject");

        email.setPlain("This is the text content");
        email.setHtml("<p>This is the HTML content</p>");

        MailerSend ms = new MailerSend();

        ms.setToken("$MAILER_SEND_API_TOKEN");

        try {
            MailerSendResponse response = ms.emails().send(email);
            System.out.println(response.messageId);
        } catch (MailerSendException e) {
            log.error("Error sending email notification to '{}': {}", e.getMessage(), e.getMessage());
        }
    }
}
