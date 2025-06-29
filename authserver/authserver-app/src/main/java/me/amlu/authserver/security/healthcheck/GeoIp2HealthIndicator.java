/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.healthcheck;

import com.maxmind.geoip2.model.AsnResponse;
import me.amlu.authserver.security.service.GeoIp2Service;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class GeoIp2HealthIndicator extends AbstractHealthIndicator {
    private final GeoIp2Service geoIp2Service;

    public GeoIp2HealthIndicator(GeoIp2Service geoIp2Service) {
        this.geoIp2Service = geoIp2Service;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            AsnResponse asnResponse = geoIp2Service.lookupAsn("8.8.8.8");
            if (asnResponse != null && asnResponse.getAutonomousSystemNumber() != null) {
                builder.up();
            } else {
                builder.down().withDetail("error", "Could not lookup test IP");
            }
        } catch (Exception e) {
            builder.down(e);
        }
    }
}
