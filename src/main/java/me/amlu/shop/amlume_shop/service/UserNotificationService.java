/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;


import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.NotificationSendingFailedException;
import me.amlu.shop.amlume_shop.payload.EmailAttachment;
import me.amlu.shop.amlume_shop.payload.CreateNotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class UserNotificationService {
    private final JavaMailSender emailSender;
    private final MethodsClient slackClient;
    
    @Value("${slack.channel}")
    private String slackChannel;

    @Value("${slack.token}")
    private String slackToken;
    
    @Value("${spring.mail.username}")
    private String emailFrom;

    public UserNotificationService(JavaMailSender emailSender, MethodsClient slackClient) {
        this.emailSender = emailSender;
        this.slackClient = slackClient;
    }

    public void sendNotification(CreateNotificationRequest request) throws NotificationSendingFailedException {
        // Add specific handling based on notification type
        switch (request.getType()) {
            case ALERT:
                // High priority handling
                sendUrgentNotification(request);
                break;
            case WARNING:
                // Medium priority handling
                sendWarningNotification(request);
                break;
            case ANNOUNCEMENT:
                // Standard handling with broader distribution
                sendAnnouncementNotification(request);
                break;
            case INFORMATIONAL:
                // Regular priority handling
                sendInformationalNotification(request);
                break;
        }
    }

    private void sendEmail(CreateNotificationRequest request) throws NotificationSendingFailedException {
        try {
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMailMessage message = new MimeMailMessage(mimeMessage);

            message.setFrom(emailFrom);
            message.setTo(request.getRecipientEmail());
            message.setSubject(request.getSubject());
            message.setText(request.getMessage());
            
            emailSender.send(mimeMessage);
            log.info("Email notification sent successfully to: {}", request.getRecipientEmail());
        } catch (MailException e) {
            log.error("Failed to send email notification", e);
            throw new NotificationSendingFailedException("Email sending failed", e);
        }
    }

    // For more complex emails with attachments or HTML
    public void sendRichEmail(CreateNotificationRequest request) throws NotificationSendingFailedException {
        try {
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(request.getRecipientEmail());
            helper.setSubject(request.getSubject());
            helper.setText(request.getMessage(), true); // true enables HTML

            // Add attachments if present
            if (request.getAttachments() != null) {
                for (EmailAttachment attachment : request.getAttachments()) {
                    helper.addAttachment(attachment.getFileName(), attachment.getFile());
                }
            }

            emailSender.send(mimeMessage);
            log.info("Rich email sent successfully to: {}", request.getRecipientEmail());

        } catch (MessagingException | MailException | jakarta.mail.MessagingException e) {
            log.error("Failed to send rich email notification", e);
            throw new NotificationSendingFailedException("Rich email sending failed", e);
        }
    }

    private void sendSlackMessage(CreateNotificationRequest request) throws NotificationSendingFailedException {
        try {
            Slack slack = Slack.getInstance();
            MethodsClient methods = slack.methods(slackToken);

            ChatPostMessageRequest slackRequest = ChatPostMessageRequest.builder()
                    .channel(slackChannel)
                    .text(request.getMessage())
                    .build();

            ChatPostMessageResponse response = methods.chatPostMessage(slackRequest);

            if (!response.isOk()) {
                log.error("Failed to send Slack message: {}", response.getError());
                throw new NotificationSendingFailedException("Slack message sending failed: " + response.getError(),new Exception());
            }

        } catch (SlackApiException | IOException e) {
            log.error("Error sending Slack message: ", e);
            throw new NotificationSendingFailedException("Failed to send Slack message", e);
        }
    }

    private List<LayoutBlock> createSlackBlocks(CreateNotificationRequest request) {
        return Arrays.asList(
            SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text("*" + request.getSubject() + "*\n" + request.getMessage())
                    .build())
                .build(),
            ActionsBlock.builder()
                .elements(Collections.singletonList(
                        ButtonElement.builder()
                                .text(PlainTextObject.builder()
                                        .text("View Details")
                                        .build())
                                .url(request.getActionUrl())
                                .style("primary")
                                .build()))
                .build()
        );
    }

    private void sendUrgentNotification(CreateNotificationRequest request) throws NotificationSendingFailedException {
        // Add urgent prefix to subject
        request.setSubject("🚨 URGENT: " + request.getSubject());

        // Send to both email and Slack
        if (request.isEmailEnabled()) {
            sendRichEmail(request);
        }
        if (request.isSlackEnabled()) {
            // Create a new request with formatted message for Slack
            CreateNotificationRequest slackRequest = CreateNotificationRequest.builder()
                    .message(formatUrgentSlackMessage(request))
                    .type(request.getType())
                    .slackEnabled(true)
                    .build();

            sendSlackMessage(slackRequest);
        }
    }

    private void sendWarningNotification(CreateNotificationRequest request) throws NotificationSendingFailedException {
        request.setSubject("⚠️ WARNING: " + request.getSubject());
        if (request.isEmailEnabled()) {
            sendRichEmail(request);
        }
        if (request.isSlackEnabled()) {
            CreateNotificationRequest slackRequest = CreateNotificationRequest.builder()
                    .message("⚠️ *WARNING*\n>" + request.getMessage())
                    .type(request.getType())
                    .slackEnabled(true)
                    .build();
            sendSlackMessage(slackRequest);
        }
    }

    private void sendAnnouncementNotification(CreateNotificationRequest request) throws NotificationSendingFailedException {
        request.setSubject("📢 ANNOUNCEMENT: " + request.getSubject());
        if (request.isEmailEnabled()) {
            sendRichEmail(request);
        }
        if (request.isSlackEnabled()) {
            CreateNotificationRequest slackRequest = CreateNotificationRequest.builder()
                    .message("⚠️ *WARNING*\n>" + request.getMessage())
                    .type(request.getType())
                    .slackEnabled(true)
                    .build();
            sendSlackMessage(slackRequest);
        }
    }

    private void sendInformationalNotification(CreateNotificationRequest request) throws NotificationSendingFailedException {
        request.setSubject("ℹ️ INFO: " + request.getSubject());
        if (request.isEmailEnabled()) {
            sendRichEmail(request);
        }
        if (request.isSlackEnabled()) {
            CreateNotificationRequest slackRequest = CreateNotificationRequest.builder()
                    .message("⚠️ *WARNING*\n>" + request.getMessage())
                    .type(request.getType())
                    .slackEnabled(true)
                    .build();
            sendSlackMessage(slackRequest);
        }
    }

    // Ignore the %n should be used. As we are using for Slack
    private String formatUrgentSlackMessage(CreateNotificationRequest request) {
        return String.format("*URGENT ALERT*\n>%s\n%s",
                request.getSubject(),
                request.getMessage());
    }

}


