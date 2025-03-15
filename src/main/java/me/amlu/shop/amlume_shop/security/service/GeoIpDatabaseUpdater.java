/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL; // Deprecated
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class GeoIpDatabaseUpdater {
    @Value("${geoip.license.key}")
    private String licenseKey;

    @Value("${geoip.database.path}")
    private String databasePath;

    @Scheduled(cron = "0 0 0 * * WED") // Run every Wednesday at midnight
    public void updateDatabase() {
        try {
            String url = String.format(
                "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=%s&suffix=tar.gz",
                    "https://git.io/GeoLite2-City.mmdb",
//                    "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=%s&suffix=tar.gz",
                licenseKey
            );

            // Download and extract new database
            downloadAndUpdateDatabase(url);
            log.info("GeoIP database updated successfully");
        } catch (Exception e) {
            log.error("Failed to update GeoIP database", e);
        }
    }

    private void downloadAndUpdateDatabase(String url) throws IOException, URISyntaxException {
        Path tempFile = Files.createTempFile("geoip", ".tar.gz");
        
        try (InputStream in = new URI(url).toURL().openStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            extractDatabase(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void extractDatabase(Path tarGzFile) throws IOException {

        // TODO: Implement tar.gz extraction
        // Implementation of tar.gz extraction

        // Copy the .mmdb file to the configured location
    }
}
