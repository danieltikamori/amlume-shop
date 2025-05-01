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

import com.maxmind.db.CHMCache;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration related to ASN (Autonomous System Number) services,
 * including dependencies like GeoIP2.
 */
@Configuration
public class AsnConfig {

    private static final Logger log = LoggerFactory.getLogger(AsnConfig.class);

    // Inject ResourceLoader to load the database file
    private final ResourceLoader resourceLoader;

    public AsnConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // Keep @Bean methods for dependencies if they are configured here
    @Bean
    public GeoIp2Service geoIp2Service(ResourceLoader resourceLoader,
                                       @Value("${geoip2.asn-database.path}") String dbPath) throws IOException, FileNotFoundException {

        log.info("Attempting to load GeoIP2 database from path: {}", dbPath);


        Resource resource = resourceLoader.getResource(dbPath);
        // *** Add check to see if the resource actually exists ***
        if (!resource.exists()) {
            log.error("GeoIP2 ASN database file not found at path: {}", dbPath);
            // Throw a specific, informative exception
            throw new FileNotFoundException("GeoIP2 ASN database file not found at path: " + dbPath +
                    ". Please ensure the file exists and the path in application.yml (geoip2.asn-database.path) uses the 'file:' prefix (e.g., 'file:/project/geoip2/GeoLite2-ASN.mmdb').");
        }

        try (InputStream dbStream = resource.getInputStream()) {

            // *** Add caching for better performance ***
            DatabaseReader reader = new DatabaseReader.Builder(dbStream)
                    .withCache(new CHMCache()) // Enable caching
                    .build();

            log.info("Successfully loaded GeoIP2 ASN database from: {}", resource.getDescription());
            return new GeoIp2ServiceImpl(reader);
        } catch (IOException e) {
            // Log error during reading
            log.error("Failed to read GeoIP2 ASN database from path: {}", dbPath, e);
            throw e; // Re-throw the exception after logging
        }
    }
}