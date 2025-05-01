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

import com.maxmind.db.InvalidDatabaseException;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.WebServiceClient;
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

@Configuration
public class GeoIp2Config {

    @Value("${geoip2.license.license-key}")
    private String licenseKey;

    @Value("${geoip2.license.account-id}")
    private int accountId;

    private static final Logger log = LoggerFactory.getLogger(GeoIp2Config.class);

    // Inject ResourceLoader to load the database file
    private final ResourceLoader resourceLoader;

    // Inject the path to the database file from application properties
    @Value("${geoip2.asn-database.path}") // e.g., classpath:geolite2/GeoLite2-ASN.mmdb or file:/path/to/GeoLite2-ASN.mmdb
    private String databasePath;

    public GeoIp2Config(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // For local MaxMind database file
//    @Value("${geoip2.asn-database.path}")
//    private String databasePath;

    @Bean
    public DatabaseReader databaseReader() throws IOException, InvalidDatabaseException {
        log.info("Loading GeoIP2 database from path: {}", databasePath);

        Resource resource = resourceLoader.getResource(databasePath);

        if (!resource.exists()) {
            log.error("GeoIP2 database file not found at path: {}", databasePath);
            throw new FileNotFoundException("GeoIP2 database file not found: " + databasePath);
        }

        try (InputStream databaseInputStream = resource.getInputStream()) {
            // Create the DatabaseReader bean using the input stream
            DatabaseReader reader = new DatabaseReader.Builder(databaseInputStream).build();
            log.info("Successfully loaded GeoIP2 database.");
            return reader;
        } catch (IOException e) {
            log.error("IOException while reading GeoIP2 database file: {}", databasePath, e);
            throw e; // Re-throw IOException
        }
        // Note: ClosedDatabaseException is typically thrown if you use a closed reader,
        // which shouldn't happen during bean creation here.
    }

    @Bean
    public WebServiceClient geoIp2Client() {
        return new WebServiceClient.Builder(accountId, licenseKey)
                .build();
    }

    // For local MaxMind database file
//    @Bean
//    public DatabaseReader geoIp2Client() throws IOException {
//        File database = new File(databasePath);
//        return new DatabaseReader.Builder(database).build();
//    }
}
