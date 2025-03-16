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

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.security.model.SecurityAlert;
import me.amlu.shop.amlume_shop.service.SlackClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
public class AlertNotificationServiceImpl implements AlertNotificationService {
    private final JavaMailSender emailSender;
    private final SlackClient slackClient;
    private final Queue<SecurityAlert> alertQueue;
    private static final int QUEUE_SIZE = 1000;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.to}")
    private String toEmail;

    public AlertNotificationServiceImpl(JavaMailSender emailSender, SlackClient slackClient) {
        this.emailSender = emailSender;
        this.slackClient = slackClient;
        this.alertQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void sendUrgentNotification(SecurityAlert alert) {
        try {
            // Send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("URGENT Security Alert: " + alert.getType());
            message.setText(buildAlertMessage(alert));
            emailSender.send(message);

            // Send Slack notification
            slackClient.sendFormattedMessage(alert);

            log.info("Urgent notification sent for alert: {}", alert.getType());
        } catch (Exception e) {
            log.error("Failed to send urgent notification", e);
        }
    }

    @Override
    public void sendNotification(SecurityAlert alert) {
        if (alertQueue.size() >= QUEUE_SIZE) {
            log.warn("Alert queue full, dropping alert: {}", alert.getType());
            return;
        }
        alertQueue.offer(alert);
    }

    @Scheduled(fixedRate = 60000) // Process queue every minute
    public void processAlertQueue() {
        List<SecurityAlert> batch = new ArrayList<>();
        SecurityAlert alert;
        
        while ((alert = alertQueue.poll()) != null && batch.size() < 100) {
            batch.add(alert);
        }

        if (!batch.isEmpty()) {
            sendBatchNotification(batch);
        }
    }

    // TOCHECK whether public or need more methods
    @Override
    public void sendBatchNotification(List<SecurityAlert> alerts) {
        try {
            StringBuilder message = new StringBuilder("*Security Alerts Batch*\n\n");
            alerts.forEach(alert ->
                    message.append(String.format("• %s - %s (%s)\n",
                            alert.getType(),
                            alert.getSeverity(),
                            alert.getUserId()))
            );
            slackClient.sendMessage(message.toString());
            log.info("Batch notification sent for {} alerts", alerts.size());
        } catch (Exception e) {
            log.error("Failed to send batch notification", e);
        }
    }

    private String buildAlertMessage(SecurityAlert alert) {
        return String.format("""
            Security Alert: %s
            User ID: %s
            Timestamp: %s
            Environment: %s
            Severity: %s
            
            Details:
            %s
            """,
            alert.getType(),
            alert.getUserId(),
            alert.getTimestamp(),
            alert.getEnvironment(),
            alert.getSeverity(),
            formatMetadata(alert.getMetadata())
        );
    }

    private String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "No additional details";
        }
        
        StringBuilder sb = new StringBuilder();
        metadata.forEach((key, value) -> 
            sb.append(String.format("- %s: %s%n", key, value))
        );
        return sb.toString();
    }

//    Link for Slack documentation:
//    https://api.slack.com/messaging/webhooks

}
