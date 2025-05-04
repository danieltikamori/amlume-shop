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

import me.amlu.shop.amlume_shop.security.enums.RiskLevel;

import java.util.*;

public class GeoVerificationResult {
    private RiskLevel risk = RiskLevel.LOW;

    private List<String> alerts = new ArrayList<>();

    private Map<String, Object> metadata = new HashMap<>();

    public GeoVerificationResult(RiskLevel risk, List<String> alerts, Map<String, Object> metadata) {
        this.risk = risk;
        this.alerts = alerts;
        this.metadata = metadata;
    }

    public GeoVerificationResult() {
    }

    private static RiskLevel $default$risk() {
        return RiskLevel.LOW;
    }

    private static List<String> $default$alerts() {
        return new ArrayList<>();
    }

    private static Map<String, Object> $default$metadata() {
        return new HashMap<>();
    }

    public static GeoVerificationResultBuilder builder() {
        return new GeoVerificationResultBuilder();
    }

    public void addAlert(String alert) {
        if (alert != null) {
            alerts.add(alert);
        }
    }

    public RiskLevel getRisk() {
        return this.risk;
    }

    public List<String> getAlerts() {
        return this.alerts;
    }

    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    public void setRisk(RiskLevel risk) {
        this.risk = risk;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof GeoVerificationResult other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$risk = this.getRisk();
        final Object other$risk = other.getRisk();
        if (!Objects.equals(this$risk, other$risk)) return false;
        final Object this$alerts = this.getAlerts();
        final Object other$alerts = other.getAlerts();
        if (!Objects.equals(this$alerts, other$alerts)) return false;
        final Object this$metadata = this.getMetadata();
        final Object other$metadata = other.getMetadata();
        return Objects.equals(this$metadata, other$metadata);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof GeoVerificationResult;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $risk = this.getRisk();
        result = result * PRIME + ($risk == null ? 43 : $risk.hashCode());
        final Object $alerts = this.getAlerts();
        result = result * PRIME + ($alerts == null ? 43 : $alerts.hashCode());
        final Object $metadata = this.getMetadata();
        result = result * PRIME + ($metadata == null ? 43 : $metadata.hashCode());
        return result;
    }

    public String toString() {
        return "GeoVerificationResult(risk=" + this.getRisk() + ", alerts=" + this.getAlerts() + ", metadata=" + this.getMetadata() + ")";
    }

    public static class GeoVerificationResultBuilder {
        private RiskLevel risk$value;
        private boolean risk$set;
        private List<String> alerts$value;
        private boolean alerts$set;
        private Map<String, Object> metadata$value;
        private boolean metadata$set;

        GeoVerificationResultBuilder() {
        }

        public GeoVerificationResultBuilder risk(RiskLevel risk) {
            this.risk$value = risk;
            this.risk$set = true;
            return this;
        }

        public GeoVerificationResultBuilder alerts(List<String> alerts) {
            this.alerts$value = alerts;
            this.alerts$set = true;
            return this;
        }

        public GeoVerificationResultBuilder metadata(Map<String, Object> metadata) {
            this.metadata$value = metadata;
            this.metadata$set = true;
            return this;
        }

        public GeoVerificationResult build() {
            RiskLevel risk$value = this.risk$value;
            if (!this.risk$set) {
                risk$value = GeoVerificationResult.$default$risk();
            }
            List<String> alerts$value = this.alerts$value;
            if (!this.alerts$set) {
                alerts$value = GeoVerificationResult.$default$alerts();
            }
            Map<String, Object> metadata$value = this.metadata$value;
            if (!this.metadata$set) {
                metadata$value = GeoVerificationResult.$default$metadata();
            }
            return new GeoVerificationResult(risk$value, alerts$value, metadata$value);
        }

        public String toString() {
            return "GeoVerificationResult.GeoVerificationResultBuilder(risk$value=" + this.risk$value + ", alerts$value=" + this.alerts$value + ", metadata$value=" + this.metadata$value + ")";
        }
    }
}
