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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for notification services (Slack, Email).
 * Binds to properties under the 'notification' prefix in application configuration.
 */
@Component
@ConfigurationProperties(prefix = "notification")
@Validated
public class NotificationProperties {

    @NotBlank(message = "Slack token cannot be blank")
    private String slackToken;

    @NotBlank(message = "Slack channel cannot be blank")
    private String slackChannel;

    @Email
    @NotBlank(message = "Default 'from' email address cannot be blank")
    private String smtpEmailFrom;

    // --- Getters ---

    public String getSlackToken() {
        return slackToken;
    }

    public String getSlackChannel() {
        return slackChannel;
    }

    public String getSmtpEmailFrom() {
        return smtpEmailFrom;
    }

    // --- Setters (Required for @ConfigurationProperties binding) ---

    public void setSlackToken(String slackToken) {
        this.slackToken = slackToken;
    }

    public void setSlackChannel(String slackChannel) {
        this.slackChannel = slackChannel;
    }

    public void setSmtpEmailFrom(String smtpEmailFrom) {
        this.smtpEmailFrom = smtpEmailFrom;
    }
}
