/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import com.maxmind.geoip2.DatabaseReader;
import me.amlu.shop.amlume_shop.security.service.GeoIp2Service;
import me.amlu.shop.amlume_shop.security.service.GeoIp2ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration related to ASN (Autonomous System Number) services,
 * including dependencies like GeoIP2.
 */
@Configuration
public class AsnConfig {

    private static final Logger log = LoggerFactory.getLogger(AsnConfig.class);

    // Keep @Bean methods for dependencies if they are configured here
    @Bean
    public GeoIp2Service geoIp2Service(ResourceLoader resourceLoader,
                                       @Value("${geoip2.database.path}") String dbPath) throws IOException {
        Resource resource = resourceLoader.getResource(dbPath);
        try (InputStream dbStream = resource.getInputStream()) {
            DatabaseReader reader = new DatabaseReader.Builder(dbStream).build();
            return new GeoIp2ServiceImpl(reader);
        }
        // Add appropriate error handling
    }

}