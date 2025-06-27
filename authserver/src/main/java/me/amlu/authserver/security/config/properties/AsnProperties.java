/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "asn")
@Data
public class AsnProperties {
    private int cacheSize = 10000;
    private Duration cacheExpiration = Duration.ofHours(24);
    private Duration staleThreshold = Duration.ofDays(30);
    private String cleanupSchedule = "0 0 3 * * *"; // Daily at 3 AM

    // Add property for reputation TTL in Redis (can default based on staleThreshold)
    private Duration reputationTtl = Duration.ofDays(31); // Default: staleThreshold + 1 day

    // Add property for reputation decay schedule (if using complex decay logic)
    private String reputationDecaySchedule = "0 1 4 * * *"; // Default: 4:01 AM daily

    // Add property for reputation decay rate (if using complex decay logic)
}
