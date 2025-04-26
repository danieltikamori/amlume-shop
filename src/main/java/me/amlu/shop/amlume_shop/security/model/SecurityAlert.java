/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

import me.amlu.shop.amlume_shop.security.enums.AlertSeverityEnum;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SecurityAlert {
    private final String userId;
    private final String type;
    private final Map<String, Object> metadata;

    private AlertSeverityEnum severity = AlertSeverityEnum.MEDIUM;

    private Instant timestamp;
    private String environment;

    public SecurityAlert(String userId, String type, Map<String, Object> metadata, AlertSeverityEnum severity, Instant timestamp, String environment) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.severity = severity != null ? severity : AlertSeverityEnum.MEDIUM;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.environment = environment != null ? environment : "production";
    }

    private static AlertSeverityEnum $default$severity() {
        return AlertSeverityEnum.MEDIUM;
    }

    public static SecurityAlertBuilder builder() {
        return new SecurityAlertBuilder();
    }

    public String getUserId() {
        return this.userId;
    }

    public String getType() {
        return this.type;
    }

    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    public AlertSeverityEnum getSeverity() {
        return this.severity;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public String getEnvironment() {
        return this.environment;
    }

    public void setSeverity(AlertSeverityEnum severity) {
        this.severity = severity;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SecurityAlert other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$userId = this.getUserId();
        final Object other$userId = other.getUserId();
        if (!Objects.equals(this$userId, other$userId)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (!Objects.equals(this$type, other$type)) return false;
        final Object this$metadata = this.getMetadata();
        final Object other$metadata = other.getMetadata();
        if (!Objects.equals(this$metadata, other$metadata)) return false;
        final Object this$severity = this.getSeverity();
        final Object other$severity = other.getSeverity();
        if (!Objects.equals(this$severity, other$severity)) return false;
        final Object this$timestamp = this.getTimestamp();
        final Object other$timestamp = other.getTimestamp();
        if (!Objects.equals(this$timestamp, other$timestamp)) return false;
        final Object this$environment = this.getEnvironment();
        final Object other$environment = other.getEnvironment();
        return Objects.equals(this$environment, other$environment);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SecurityAlert;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final Object $metadata = this.getMetadata();
        result = result * PRIME + ($metadata == null ? 43 : $metadata.hashCode());
        final Object $severity = this.getSeverity();
        result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
        final Object $timestamp = this.getTimestamp();
        result = result * PRIME + ($timestamp == null ? 43 : $timestamp.hashCode());
        final Object $environment = this.getEnvironment();
        result = result * PRIME + ($environment == null ? 43 : $environment.hashCode());
        return result;
    }

    public String toString() {
        return "SecurityAlert(userId=" + this.getUserId() + ", type=" + this.getType() + ", metadata=" + this.getMetadata() + ", severity=" + this.getSeverity() + ", timestamp=" + this.getTimestamp() + ", environment=" + this.getEnvironment() + ")";
    }

    public static class SecurityAlertBuilder {
        private String userId;
        private String type;
        private Map<String, Object> metadata;
        private AlertSeverityEnum severity$value;
        private boolean severity$set;
        private Instant timestamp;
        private String environment;

        SecurityAlertBuilder() {
        }

        public SecurityAlertBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public SecurityAlertBuilder type(String type) {
            this.type = type;
            return this;
        }

        public SecurityAlertBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SecurityAlertBuilder severity(AlertSeverityEnum severity) {
            this.severity$value = severity;
            this.severity$set = true;
            return this;
        }

        public SecurityAlertBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SecurityAlertBuilder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public SecurityAlert build() {
            AlertSeverityEnum severity$value = this.severity$value;
            if (!this.severity$set) {
                severity$value = SecurityAlert.$default$severity();
            }
            return new SecurityAlert(this.userId, this.type, this.metadata, severity$value, this.timestamp, this.environment);
        }

        public String toString() {
            return "SecurityAlert.SecurityAlertBuilder(userId=" + this.userId + ", type=" + this.type + ", metadata=" + this.metadata + ", severity$value=" + this.severity$value + ", timestamp=" + this.timestamp + ", environment=" + this.environment + ")";
        }
    }
}
