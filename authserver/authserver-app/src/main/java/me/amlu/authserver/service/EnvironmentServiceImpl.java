/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Service implementation for retrieving and managing application environment information.
 * This class provides methods to determine the current active Spring profile
 * and check if the application is running in specific environments like
 * production, development, local, or staging.
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * @Autowired
 * private EnvironmentService environmentService;
 *
 * public void someMethod() {
 *     String currentEnv = environmentService.getCurrentEnvironment();
 *     System.out.println("Current environment: " + currentEnv);
 *
 *     if (environmentService.isProduction()) {
 *         System.out.println("Running in production mode.");
 *     }
 * }
 * }</pre>
 *
 * <p>Important Notes:</p>
 * <ul>
 *     <li>The environment determination is based on Spring's active profiles.</li>
 *     <li>If multiple profiles are active, only the first one is considered for {@code getCurrentEnvironment()}.</li>
 *     <li>If no active profiles are explicitly set, it defaults to "LOCAL".</li>
 *     <li>Environment names are case-insensitive when compared internally but returned in uppercase.</li>
 * </ul>
 */
@Component
public class EnvironmentServiceImpl implements EnvironmentService {
    private final Environment springEnvironment;

    /**
     * Constructs an {@code EnvironmentServiceImpl} with the provided Spring {@link Environment}.
     *
     * @param springEnvironment The Spring environment instance, typically injected by the framework.
     */
    public EnvironmentServiceImpl(Environment springEnvironment) {
        this.springEnvironment = springEnvironment;
    }

    /**
     * Retrieves the current active environment of the application.
     * This method first checks for active Spring profiles. If multiple profiles are active,
     * it returns the first one. If no profiles are active, it attempts to read the
     * "spring.profiles.active" property. If that also yields no result, it defaults to "LOCAL".
     * The returned environment name is always in uppercase.
     *
     * @return A string representing the current environment (e.g., "PRODUCTION", "DEVELOPMENT", "LOCAL", "STAGING").
     */
    @Override
    public String getCurrentEnvironment() {
        String[] activeProfiles = springEnvironment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0].toUpperCase(Locale.ROOT);
        }
        // Fallback to property if no active profiles are set, or default to LOCAL
        return springEnvironment.getProperty("spring.profiles.active", "local").toUpperCase(Locale.ROOT);
    }

    /**
     * Checks if the current environment is "PRODUCTION".
     * The comparison is case-insensitive.
     *
     * @return {@code true} if the current environment is production, {@code false} otherwise.
     */
    public boolean isProduction() {
        // Using equalsIgnoreCase for robustness against different casing in profile names
        return "PRODUCTION".equalsIgnoreCase(getCurrentEnvironment());
    }

    public boolean isDevelopment() {
        return "DEVELOPMENT".equals(getCurrentEnvironment());
    }

    public boolean isLocal() {
        return "LOCAL".equals(getCurrentEnvironment());
    }

    public boolean isStaging() {
        return "STAGING".equals(getCurrentEnvironment());
    }
}
