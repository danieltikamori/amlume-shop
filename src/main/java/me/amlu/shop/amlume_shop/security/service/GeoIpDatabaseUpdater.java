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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

@Service
@Slf4j
public class GeoIpDatabaseUpdater {

//    // Check if accountId is needed
//    @Value("${geoip.license.account-id}")
//    private String accountId;

    @Value("${geoip.license.key}")
    private String licenseKey;

    @Value("${geoip.database.path}")
    private String databasePath;

    @Scheduled(cron = "0 0 0 * * WED") // Run every Wednesday at midnight
    public void updateDatabase() {
        try {
            String url = String.format(
                    "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=%s&suffix=tar.gz",
                    licenseKey
            );

//            String url = String.format(
//                "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=%s&suffix=tar.gz",
//                    "https://git.io/GeoLite2-City.mmdb",
////                    "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=%s&suffix=tar.gz",
//                licenseKey
//            );

            // Download and extract new database
            downloadAndUpdateDatabase(url);
            log.info("GeoIP database updated successfully");
        } catch (Exception e) {
            log.error("Failed to update GeoIP database", e);
        }
    }

    private void downloadAndUpdateDatabase(String url) throws IOException, URISyntaxException {
//        Path localDatabasePath = Paths.get(this.databasePath);
//        Path tempFile = localDatabasePath.resolve("geoip.tar.gz");
        Path tempFile = Paths.get(this.databasePath).resolve("geoip.tar.gz");

        try (InputStream in = new URI(url).toURL().openStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            extractDatabase(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
            log.info("Temporary file deleted: {}, path: {} at time: {}", tempFile, tempFile.toAbsolutePath(), Instant.now());
        }
    }

    private void extractDatabase(Path tarGzFile) throws IOException {
        // Implementation of tar.gz extraction
        Path localDatabasePath = tarGzFile.getParent();
        try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarGzFile.toFile())))) {
            TarArchiveEntry entry;
            while ((entry = tarArchiveInputStream.getNextEntry()) != null) {
                Path filePath = localDatabasePath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    createDirectory(filePath);
                    continue;
                }
                copyFile(tarArchiveInputStream, filePath);
                if (filePath.getFileName().toString().endsWith(".mmdb")) {
                    log.info("Extracted GeoIP database: {}", filePath);
                }
            }
        }
    }

    private void createDirectory(Path filePath) {
        try {
            Files.createDirectories(filePath);
        } catch (IOException e) {
            log.error("Error creating directory", e);
        }
    }

    private void copyFile(TarArchiveInputStream tarArchiveInputStream, Path filePath) {
        try {
            Files.copy(tarArchiveInputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error copying file", e);
        }

//        try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarGzFile.toFile())))) {
//            TarArchiveEntry entry;
//            while ((entry = tarArchiveInputStream.getNextEntry()) != null) {
//                Path filePath = tarGzFile.getParent().resolve(entry.getName());
//                if (entry.isDirectory()) {
//                    Files.createDirectories(filePath);
//                } else {
//                    Files.copy(tarArchiveInputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
//                }
//            }
//        }
    }
}
