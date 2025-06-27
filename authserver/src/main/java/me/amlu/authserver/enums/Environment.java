/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.enums;

public enum Environment {
    LOCAL,
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
    DR;

    private static Environment currentEnvironment;

    static {
        String env = System.getProperty("spring.profiles.active", "LOCAL").toUpperCase();
        try {
            currentEnvironment = Environment.valueOf(env);
        } catch (IllegalArgumentException e) {
            currentEnvironment = LOCAL;
        }
    }

    public static Environment getCurrentEnvironment() {
        if (currentEnvironment == null) {
            String springProfile = System.getProperty("spring.profiles.active", "LOCAL").toUpperCase();
            try {
                currentEnvironment = Environment.valueOf(springProfile);
            } catch (IllegalArgumentException e) {
                currentEnvironment = LOCAL;
            }
        }
        return currentEnvironment;
    }


    public static void setCurrentEnvironment(Environment environment) {
        currentEnvironment = environment;
    }

    public boolean isProduction() {
        return this == PRODUCTION;
    }

    public boolean isDevelopment() {
        return this == DEVELOPMENT;
    }

    public boolean isLocal() {
        return this == LOCAL;
    }

    public boolean isStaging() {
        return this == STAGING;
    }

    public boolean isDR() {
        return this == DR;
    }
}
