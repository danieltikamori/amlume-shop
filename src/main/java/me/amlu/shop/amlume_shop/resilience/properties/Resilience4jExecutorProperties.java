/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Resilience4j executor instances.
 */

@Component
@ConfigurationProperties(prefix = "resilience4j.executor.instances.default")
@Validated
public class Resilience4jExecutorProperties {

    /**
     * The core pool size.
     * <p>
     * This property is used to specify the core pool size of the thread pool.
     * The default value is 10.
     * </p>
     */
    private int corePoolSize = 4;

    /**
     * The maximum number of concurrent calls.
     * <p>
     * This property is used to specify the maximum number of concurrent calls that can be made.
     * The default value is 10.
     * </p>
     */
    private int maxConcurrentCalls = 100;


    // --- Getters ---

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    // --- Setters ---

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public void setMaxConcurrentCalls(int maxConcurrentCalls) {
        this.maxConcurrentCalls = maxConcurrentCalls;
    }
}
