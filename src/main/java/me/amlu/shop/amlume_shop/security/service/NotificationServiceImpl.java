/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Properties;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(NotificationServiceImpl.class);
    //    Implement notification service
    // System.getenv("SLACK_API_TOKEN") - get slack api token from environment variable of the OS. Immutable.
    //System.getProperty("SLACK_API_TOKEN") - get slack api token from system property. Contained within JVM. Mutable.
    final String SLACK_API_TOKEN = System.getenv("SLACK_API_TOKEN");
    final String SLACK_CHANNEL = System.getenv("SLACK_CHANNEL");
    final String EMAIL_SERVICE_PROVIDER = System.getenv("SMTP_HOST");
    final String EMAIL_FROM = System.getenv("SMTP_USERNAME");
    final String EMAIL_PASSWORD = System.getenv("SMTP_PASSWORD");
    final String EMAIL_SMTP_PORT = System.getenv("SMTP_PORT");
    final String EMAIL_AUTH_ENABLED = System.getenv("SMTP_AUTH_ENABLED");
    final String EMAIL_STARTTLS_ENABLED = System.getenv("SMTP_STARTTLS_ENABLED");


    @Override
    public void sendNotification(String message, String email) {
        // Send notification to Slack
        Slack slack = Slack.getInstance();

        try {
            slack.methods().chatPostMessage(ChatPostMessageRequest.builder().channel(SLACK_CHANNEL).token(SLACK_API_TOKEN).text(message).build());
        } catch (SlackApiException | IOException e) {
            log.atError().log("Error sending notification to Slack: " + e.getMessage());
        }

        // Send email notification
        Properties props = new Properties();
        props.put("mail.smtp.host", EMAIL_SERVICE_PROVIDER);
        props.put("mail.smtp.port", EMAIL_SMTP_PORT);
        props.put("mail.smtp.auth", EMAIL_AUTH_ENABLED);
        props.put("mail.smtp.starttls.enable", EMAIL_STARTTLS_ENABLED);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(EMAIL_FROM));
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            mimeMessage.setSubject("Notification");
            mimeMessage.setText(message);

            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Error sending email notification: {}", e.getMessage());
        }
    }
}
