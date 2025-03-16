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

import lombok.Builder;
import lombok.Data;
import me.amlu.shop.amlume_shop.security.enums.AlertSeverityEnum;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@Builder
public class SecurityAlert {
    private final String userId;
    private final String type;
    private final Map<String, Object> metadata;
    
    @Builder.Default
    private AlertSeverityEnum severity = AlertSeverityEnum.MEDIUM;
    
    private Instant timestamp;
    private String environment;

    public SecurityAlert(String userId, String type, Map<String, Object> metadata) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
}
