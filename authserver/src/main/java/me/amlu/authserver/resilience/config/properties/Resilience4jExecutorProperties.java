/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.resilience.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Resilience4j executor instances.
 * <p>
 * This class is used to configure the properties of the executor instances
 * for Resilience4j.
 * <p>
 * It includes properties such as core pool size and maximum number of concurrent calls.
 *
 * @author Daniel Itiro Tikamori
 */

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
    @Min(value = 1, message = "corePoolSize must be greater than or equal to 1")
    private int corePoolSize = 4;

    /**
     * The maximum number of concurrent calls.
     * <p>
     * This property is used to specify the maximum number of concurrent calls that can be made.
     * The default value is 10.
     * </p>
     */
    @Min(value = 1, message = "maxConcurrentCalls must be greater than or equal to 1")
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
