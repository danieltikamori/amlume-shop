/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;

import org.springframework.stereotype.Component;

@Component
public class EnvironmentServiceImpl implements EnvironmentService {
    private final org.springframework.core.env.Environment springEnvironment;

    public EnvironmentServiceImpl(org.springframework.core.env.Environment springEnvironment) {
        this.springEnvironment = springEnvironment;
    }

    @Override
    public String getCurrentEnvironment() {
        String[] activeProfiles = springEnvironment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0].toUpperCase();
        }
        return springEnvironment.getProperty("spring.profiles.active", "LOCAL").toUpperCase();
    }

    public boolean isProduction() {
        return "PRODUCTION".equals(getCurrentEnvironment());
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
