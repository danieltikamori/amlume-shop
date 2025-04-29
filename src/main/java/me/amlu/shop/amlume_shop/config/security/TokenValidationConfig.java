/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
@ConfigurationProperties(prefix = "token.validation")
public class TokenValidationConfig {
    /**
     * The maximum allowed clock skew for token validation.
     * This is used to account for potential time differences between the server and the client.
     * Default is 5 minutes.
     */
    private Duration clockSkewTolerance;
    private String issuer;
    private String audience;


    public TokenValidationConfig() {
    }

    public Duration getClockSkewTolerance() {
        return clockSkewTolerance;
    }

    public void setClockSkewTolerance(Duration clockSkewTolerance) {
        this.clockSkewTolerance = clockSkewTolerance;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }


    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TokenValidationConfig other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$clockSkewTolerance = this.getClockSkewTolerance();
        final Object other$clockSkewTolerance = other.getClockSkewTolerance();
        if (!Objects.equals(this$clockSkewTolerance, other$clockSkewTolerance))
            return false;
        final Object this$issuer = this.getIssuer();
        final Object other$issuer = other.getIssuer();
        if (!Objects.equals(this$issuer, other$issuer)) return false;
        final Object this$audience = this.getAudience();
        final Object other$audience = other.getAudience();
        return Objects.equals(this$audience, other$audience);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TokenValidationConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $clockSkewTolerance = this.getClockSkewTolerance();
        result = result * PRIME + ($clockSkewTolerance == null ? 43 : $clockSkewTolerance.hashCode());
        final Object $issuer = this.getIssuer();
        result = result * PRIME + ($issuer == null ? 43 : $issuer.hashCode());
        final Object $audience = this.getAudience();
        result = result * PRIME + ($audience == null ? 43 : $audience.hashCode());
        return result;
    }

    public String toString() {
        return "TokenValidationConfig(clockSkewTolerance=" + this.getClockSkewTolerance() + ", issuer=" + this.getIssuer() + ", audience=" + this.getAudience() + ")";
    }
}
