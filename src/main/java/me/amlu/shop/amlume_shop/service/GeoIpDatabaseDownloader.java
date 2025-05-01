/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service; // Or a suitable package

import jakarta.annotation.PostConstruct;
import me.amlu.shop.amlume_shop.config.properties.GeoIp2Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service("geoIpDatabaseDownloader") // Give it a specific bean name
public class GeoIpDatabaseDownloader {

    private static final Logger log = LoggerFactory.getLogger(GeoIpDatabaseDownloader.class);
    private static final Duration MAX_AGE_BEFORE_UPDATE_CHECK = Duration.ofDays(7); // Check for updates weekly

    private final GeoIp2Properties geoIp2Properties;
    // Optional: Inject RestTemplate or WebClient if implementing download directly

    public GeoIpDatabaseDownloader(GeoIp2Properties geoIp2Properties) {
        this.geoIp2Properties = geoIp2Properties;
    }

    @PostConstruct
    public void checkAndPrepareDatabases() {
        log.info("Checking GeoIP2 database directory and files...");
        Path dbDirectory = Paths.get(geoIp2Properties.getDatabaseDirectory());

        try {
            // 1. Ensure Directory Exists
            if (!Files.exists(dbDirectory)) {
                log.info("GeoIP2 database directory does not exist. Creating: {}", dbDirectory);
                Files.createDirectories(dbDirectory);
            } else if (!Files.isDirectory(dbDirectory)) {
                log.error("Configured GeoIP2 database path exists but is not a directory: {}", dbDirectory);
                // Fail fast - application likely won't work
                throw new IllegalStateException("GeoIP2 path is not a directory: " + dbDirectory);
            } else {
                 log.debug("GeoIP2 database directory exists: {}", dbDirectory);
            }

            // 2. Check Individual Database Files (Example for City DB)
            checkAndDownloadDatabase(geoIp2Properties.getCityDatabase().getPath(), "GeoLite2-City");
            checkAndDownloadDatabase(geoIp2Properties.getAsnDatabase().getPath(), "GeoLite2-ASN");
            checkAndDownloadDatabase(geoIp2Properties.getCountryDatabase().getPath(), "GeoLite2-Country");

            log.info("GeoIP2 database check completed.");

        } catch (IOException e) {
            log.error("Failed to create GeoIP2 database directory: {}", dbDirectory, e);
            // Depending on requirements, you might want to throw an exception here
            // to prevent the application from starting without the directory.
            throw new RuntimeException("Failed to initialize GeoIP2 database directory", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during GeoIP2 database preparation", e);
            throw new RuntimeException("Failed to prepare GeoIP2 databases", e);
        }
    }

    private void checkAndDownloadDatabase(String filePathStr, String dbName) {
        Path filePath = Paths.get(filePathStr);
        boolean needsDownload = false;

        if (!Files.exists(filePath)) {
            log.warn("GeoIP2 database file not found: {}. Download required.", filePath);
            needsDownload = true;
        } else {
            log.info("GeoIP2 database file found: {}", filePath);
            // Optional: Check file age for updates
            try {
                Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();
                if (Instant.now().isAfter(lastModified.plus(MAX_AGE_BEFORE_UPDATE_CHECK))) {
                    log.warn("GeoIP2 database file {} is older than {}. Triggering update.", filePath, MAX_AGE_BEFORE_UPDATE_CHECK);
                    needsDownload = true;
                }
            } catch (IOException e) {
                 log.error("Could not determine age of GeoIP2 database file: {}", filePath, e);
                 // Decide if you want to force download on error or not
                 // needsDownload = true;
            }
        }

        if (needsDownload) {
            log.info("Attempting to download/update {} database...", dbName);
            boolean success = runGeoIpUpdateTool(); // Use external tool
            // OR: implementDirectDownload(filePath, dbName); // Use RestTemplate/WebClient

            if (!success) {
                 log.error("Failed to download/update {} database. The application might not function correctly.", dbName);
                 // Decide if failure is critical
                 // throw new RuntimeException("Failed to download required GeoIP database: " + dbName);
            } else {
                 log.info("Successfully downloaded/updated {} database.", dbName);
            }
        }
    }

    /**
     * Executes the external geoipupdate command-line tool.
     * Relies on the tool being installed and configured correctly
     * (e.g., via GeoIP.conf pointing to the properties).
     *
     * @return true if the command executes successfully (exit code 0), false otherwise.
     */
    private boolean runGeoIpUpdateTool() {
        // Ensure GeoIP.conf is configured correctly with AccountID, LicenseKey, EditionIDs, and DatabaseDirectory
        // from geoIp2Properties. You might need to generate this file dynamically if not present.
        // For simplicity, we assume geoipupdate is configured externally or via a standard GeoIP.conf location.

        // Path to the geoipupdate executable (adjust if necessary)
        String geoipUpdateCommand = "geoipupdate"; // Assumes it's in the system PATH

        // Add arguments, e.g., -v for verbose, -f path/to/GeoIP.conf if needed
        ProcessBuilder processBuilder = new ProcessBuilder(geoipUpdateCommand, "-v", "-d", geoIp2Properties.getDatabaseDirectory());
        // Optional: Redirect output/error streams
        processBuilder.redirectErrorStream(true); // Merge stderr into stdout

        log.info("Executing geoipupdate command...");
        try {
            Process process = processBuilder.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                    log.debug("[geoipupdate] {}", line); // Log output line by line
                }
            }

            // Wait for the process to complete with a timeout
            boolean finished = process.waitFor(2, TimeUnit.MINUTES); // 2-minute timeout

            if (!finished) {
                 log.error("geoipupdate command timed out after 2 minutes.");
                 process.destroyForcibly();
                 return false;
            }

            int exitCode = process.exitValue();
            log.info("geoipupdate finished with exit code: {}", exitCode);

            if (exitCode != 0) {
                log.error("geoipupdate failed. Output:\n{}", output);
                return false;
            }

            return true;

        } catch (IOException e) {
            log.error("IOException while running geoipupdate. Is the tool installed and in PATH?", e);
            return false;
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for geoipupdate to finish.", e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
             log.error("Unexpected error running geoipupdate.", e);
             return false;
        }
    }

    // Placeholder for direct download implementation (more complex)
    /*
    private boolean implementDirectDownload(Path targetFile, String dbName) {
        log.warn("Direct download implementation is complex and not fully provided.");
        // 1. Construct the correct MaxMind download URL (requires knowing the format, often involves account ID/license)
        //    Example (likely incorrect, check MaxMind docs):
        //    String downloadUrl = "https://download.maxmind.com/app/geoip_download?edition_id=" + dbName + "&license_key=" + geoIp2Properties.getLicense().getLicenseKey() + "&suffix=tar.gz"; // Or .mmdb directly?
        // 2. Use RestTemplate/WebClient to perform GET request
        // 3. Handle authentication (possibly Basic Auth or query params with license)
        // 4. Stream the response body directly to the targetFile path.
        // 5. Handle potential compressed formats (e.g., .tar.gz) - requires unzipping/untarring.
        // 6. Implement robust error handling (network issues, auth errors, file write errors).
        return false; // Return false as it's not implemented
    }
    */
}