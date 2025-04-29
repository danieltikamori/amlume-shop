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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.amlu.shop.amlume_shop.security.model.SecurityAlert;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SlackClient {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SlackClient.class);
    private final WebClient webClient;
    private final String webhookUrl;
    private final ObjectMapper objectMapper;

    public SlackClient(@Value("${notification.slack.webhook-url}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void sendMessage(String message) {
        try {
            SlackMessage slackMessage = new SlackMessage(message);
            String jsonPayload = objectMapper.writeValueAsString(slackMessage);

            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(jsonPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            response -> log.debug("Slack message sent successfully"),
                            error -> log.error("Failed to send Slack message: {}", error.getMessage())
                    );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Slack message", e);
        }
    }

    public void sendFormattedMessage(SecurityAlert alert) {
        try {
            SlackMessage slackMessage = buildFormattedMessage(alert);
            String jsonPayload = objectMapper.writeValueAsString(slackMessage);

            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(jsonPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            response -> log.debug("Formatted Slack message sent successfully"),
                            error -> log.error("Failed to send formatted Slack message: {}", error.getMessage())
                    );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize formatted Slack message", e);
        }
    }

    private SlackMessage buildFormattedMessage(SecurityAlert alert) {
        return SlackMessage.builder()
                .text("*Security Alert*")
                .blocks(List.of(
                        SlackBlock.builder()
                                .type("section")
                                .text(SlackText.builder()
                                        .type("mrkdwn")
                                        .text(String.format("*Alert Type:* %s\n*Severity:* %s\n*User ID:* %s",
                                                alert.getType(),
                                                alert.getSeverity(),
                                                alert.getUserId()))
                                        .build())
                                .build(),
                        SlackBlock.builder()
                                .type("divider")
                                .build(),
                        SlackBlock.builder()
                                .type("section")
                                .text(SlackText.builder()
                                        .type("mrkdwn")
                                        .text(formatMetadata(alert.getMetadata()))
                                        .build())
                                .build()
                ))
                .build();
    }

    private String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "*No additional details*";
        }

        StringBuilder sb = new StringBuilder("*Details:*\n");
        metadata.forEach((key, value) ->
//                sb.append(String.format("• *%s:* %s\n", key, value)));
                sb.append(String.format("• *%s:* %s%n", key, value))); // %n is platform-independent newline
        return sb.toString();
    }
}

// Slack webhooks documentation:
//https://api.slack.com/messaging/webhooks

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SlackMessage {
    private String text;
    @Builder.Default
    private List<SlackBlock> blocks = new ArrayList<>();

    public SlackMessage(String text) {
        this.text = text;
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SlackBlock {
    private String type;
    private SlackText text;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SlackText {
    private String type;
    private String text;
}
