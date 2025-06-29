/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import jakarta.annotation.PostConstruct;

import me.amlu.authserver.security.config.properties.GeoIp2Properties;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Service responsible for downloading and managing GeoIP2 databases from MaxMind.
 * This service ensures that the necessary GeoIP2 database files (City, ASN, Country)
 * are present and up-to-date in the configured directory. It handles initial
 * directory creation, file existence checks, age-based updates, and secure
 * download/extraction of the .mmdb files from MaxMind's tar.gz archives.
 *
 * <p>The download process requires a valid MaxMind Account ID and License Key,
 * which should be configured in the application properties.
 *
 * <p>This service is initialized automatically on application startup via the
 * {@link PostConstruct} annotation, attempting to prepare the databases.
 * Failures during database preparation are logged but do not prevent the
 * application from starting, though GeoIP features may be unavailable.
 *
 * <p><b>Usage:</b>
 * Configure `application.properties` or `application.yml` with:
 * <ul>
 *     <li>`geoip2.database-directory`: Path where .mmdb files will be stored.</li>
 *     <li>`geoip2.license.account-id`: Your MaxMind Account ID.</li>
 *     <li>`geoip2.license.license-key`: Your MaxMind License Key.</li>
 * </ul>
 *
 * <p>Example configuration:
 * <pre>
 * geoip2.database-directory=/var/lib/geoip
 * geoip2.license.account-id=123456
 * geoip2.license.license-key=your_license_key_here
 * </pre>
 */
@Service("geoIpDatabaseDownloader")
public class GeoIpDatabaseDownloader {

    private static final Logger log = LoggerFactory.getLogger(GeoIpDatabaseDownloader.class);
    private static final Duration MAX_AGE_BEFORE_UPDATE_CHECK = Duration.ofDays(7);
    private static final String MAXMIND_DOWNLOAD_BASE_URL = "https://download.maxmind.com/app/geoip_download";
    private static final String DOWNLOAD_SUFFIX = "tar.gz"; // MaxMind provides .tar.gz archives

    private final GeoIp2Properties geoIp2Properties; // Configuration properties for GeoIP2
    private final RestTemplate restTemplate; // Used for making HTTP requests to download databases

    /**
     * Constructs a new {@code GeoIpDatabaseDownloader} with the specified properties and RestTemplate.
     *
     * @param geoIp2Properties The configuration properties for GeoIP2.
     * @param restTemplate     The RestTemplate used for HTTP requests.
     */
    public GeoIpDatabaseDownloader(GeoIp2Properties geoIp2Properties, RestTemplate restTemplate) {
        this.geoIp2Properties = geoIp2Properties;
        this.restTemplate = restTemplate;
    }

    /**
     * Initializes the service by checking and preparing the GeoIP2 database directory
     * and individual database files. This method is automatically called after
     * dependency injection is complete.
     *
     * <p>It performs the following steps:
     * <ol>
     *     <li>Validates and resolves the configured database directory path.</li>
     *     <li>Creates the database directory if it does not exist.</li>
     *     <li>Checks each configured database (City, ASN, Country) for existence and age.</li>
     *     <li>Initiates a download if a database is missing or outdated.</li>
     * </ol>
     * Errors during this process are logged, but the application will continue to start.
     * However, GeoIP features may be impaired or unavailable if databases cannot be prepared.
     */
    @PostConstruct
    public void checkAndPrepareDatabases() {
        log.info("Attempting to check and prepare GeoIP2 database directory and files...");
        Path dbDirectory = null; // Initialize to null

        try {
            // --- Safely get Path ---
            try {
                dbDirectory = Paths.get(geoIp2Properties.getDatabaseDirectory());
            } catch (Exception e) {
                // Catch potential InvalidPathException or others during path creation
                log.error("CRITICAL: Invalid GeoIP2 database directory path configured: '{}'. GeoIP features will be unavailable.",
                        geoIp2Properties.getDatabaseDirectory(), e);
                // Cannot proceed without a valid directory path object
                return; // Exit PostConstruct
            }

            // --- Ensure Directory Exists ---
            if (!Files.exists(dbDirectory)) {
                log.info("GeoIP2 database directory does not exist. Attempting to create: {}", dbDirectory);
                try {
                    Files.createDirectories(dbDirectory);
                    log.info("Successfully created GeoIP2 database directory: {}", dbDirectory);
                } catch (IOException | SecurityException e) {
                    // Log error but DO NOT throw RuntimeException - allow startup to continue
                    log.error("Failed to create GeoIP2 database directory: {}. GeoIP features might be unavailable if files cannot be downloaded/placed.", dbDirectory, e);
                    // Continue, maybe the directory gets created later, or files already exist elsewhere?
                }
            } else if (!Files.isDirectory(dbDirectory)) {
                // Log error but DO NOT throw RuntimeException
                log.error("Configured GeoIP2 database path exists but is not a directory: {}. GeoIP features will be unavailable.", dbDirectory);
                // Cannot proceed if it's not a directory
                return; // Exit PostConstruct
            } else {
                log.debug("GeoIP2 database directory exists: {}", dbDirectory);
            }

            // --- Check Individual Database Files ---
            // These calls now only log errors on download failure, they don't throw exceptions upwards
            checkAndDownloadDatabase(geoIp2Properties.getCityDatabase().getPath(), "GeoLite2-City");
            checkAndDownloadDatabase(geoIp2Properties.getAsnDatabase().getPath(), "GeoLite2-ASN");
            checkAndDownloadDatabase(geoIp2Properties.getCountryDatabase().getPath(), "GeoLite2-Country");

            log.info("GeoIP2 database preparation check completed.");

        } catch (Exception e) {
            // Catch any other unexpected errors during the preparation phase
            // Log error but DO NOT throw RuntimeException
            log.error("An unexpected error occurred during GeoIP2 database preparation. GeoIP features may be affected.", e);
        }
    }

    /**
     * Checks the existence and age of a specific GeoIP2 database file and initiates
     * a download if necessary.
     *
     * @param filePathStr The string representation of the path to the database file.
     * @param editionId The MaxMind edition ID for the database (e.g., "GeoLite2-City").
     *                  This is used for logging and constructing the download URL.
     */
    private void checkAndDownloadDatabase(String filePathStr, String editionId) {
        Path filePath = null; // Initialize to null
        try {
            // --- Safely get Path ---
            try {
                filePath = Paths.get(filePathStr);
            } catch (Exception e) {
                log.error("Invalid file path configured for GeoIP DB '{}': '{}'. Cannot check/download.", editionId, filePathStr, e);
                return; // Cannot proceed with this file
            }

            boolean needsDownload = false;
            boolean fileExists = false;

            // --- Check File Existence and Age (handle potential errors) ---
            try {
                fileExists = Files.exists(filePath);
                if (!fileExists) {
                    log.warn("GeoIP2 database file not found: {}. Download required.", filePath);
                    needsDownload = true;
                } else {
                    log.info("GeoIP2 database file found: {}", filePath);
                    // Check age only if file exists
                    Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();
                    if (Instant.now().isAfter(lastModified.plus(MAX_AGE_BEFORE_UPDATE_CHECK))) {
                        log.warn("GeoIP2 database file {} is older than {}. Triggering update.", filePath, MAX_AGE_BEFORE_UPDATE_CHECK);
                        needsDownload = true;
                    }
                }
            } catch (IOException | SecurityException e) {
                log.error("Error checking existence or age of GeoIP2 database file: {}. Assuming download is needed.", filePath, e);
                needsDownload = true; // Attempt download if we can't check
            }

            // --- Attempt Download if Needed ---
            if (needsDownload) {
                log.info("Attempting to download/update {} database (Edition ID: {})...", filePath.getFileName(), editionId);
                boolean success = downloadDatabaseDirectly(filePath, editionId); // This method now only returns boolean

                if (!success) {
                    // Log error, but don't throw exception
                    log.error("Failed to download/update {} database. Service might rely on existing (potentially outdated) file or fail if file is missing.", filePath.getFileName());
                } else {
                    log.info("Successfully downloaded/updated {} database.", filePath.getFileName());
                }
            }
        } catch (Exception e) {
            // Catch any other unexpected errors during check/download for a specific file
            log.error("Unexpected error processing GeoIP database file check/download for edition '{}', path '{}'.", editionId, filePathStr, e);
        }
    }

    /**
     * Downloads a specific GeoIP2 database from MaxMind's servers.
     * This method constructs the download URL using the configured MaxMind Account ID
     * and License Key, performs the HTTP GET request, and then extracts the .mmdb
     * file from the downloaded .tar.gz archive.
     *
     * @param targetFile The {@link Path} where the final .mmdb file should be saved.
     * @param editionId The MaxMind edition ID (e.g., "GeoLite2-City") for the database to download.
     * @return {@code true} if the download and extraction were successful, {@code false} otherwise.
     */
    private boolean downloadDatabaseDirectly(Path targetFile, String editionId) {
        // --- 1. Construct URL & Check Credentials (same as before) ---
        String accountId = geoIp2Properties.getLicense().getAccountId();
        String licenseKey = geoIp2Properties.getLicense().getLicenseKey();

        if (accountId.isBlank() || licenseKey.isBlank()) {
            log.error("Cannot download GeoIP database '{}': Account ID or License Key is missing in configuration.", editionId);
            return false;
        }

        String downloadUrl;
        try {
            downloadUrl = UriComponentsBuilder.fromUriString(MAXMIND_DOWNLOAD_BASE_URL)
                    .queryParam("edition_id", editionId)
                    .queryParam("license_key", licenseKey) // License key is required for download
                    .queryParam("suffix", DOWNLOAD_SUFFIX)
                    .toUriString();
        } catch (Exception e) {
            log.error("Failed to build download URL for edition '{}'", editionId, e);
            return false;
        }

        // Log the attempt without sensitive information
        log.info("Attempting download for '{}' from Maxmind GeoIp2 servers.", editionId);

        // --- 2. Prepare Request Callback and Response Extractor ---
        final RequestCallback requestCallback = request -> {
        }; // No specific headers needed usually

        final ResponseExtractor<Boolean> responseExtractor = response -> {
            // Check for successful status code
            if (!response.getStatusCode().is2xxSuccessful()) {
                String responseBody = "";
                try {
                    responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                } catch (IOException ignored) { /* Ignore error reading error body */ }
                log.error("Download failed for '{}' with status: {} {}. Response: {}", editionId, response.getStatusCode(), response.getStatusText(), responseBody);
                // Do not throw exception here, just return false from extractor
                return false;
            }

            Path tempTarGz = null;
            try {
                tempTarGz = Files.createTempFile("geoip2_" + editionId + "-", ".tar.gz");
                log.debug("Streaming download response for '{}' to temporary file: {}", editionId, tempTarGz);
                Files.copy(response.getBody(), tempTarGz, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Successfully saved downloaded archive for '{}' to temporary file: {}", editionId, tempTarGz);

                // Extract the tar.gz file to the target directory
                log.debug("Extracting .mmdb file from archive: {}", tempTarGz);
                boolean extracted = extractMmdbFromTarGz(tempTarGz, targetFile, editionId);

                if (extracted) {
                    log.debug("Successfully extracted and saved '{}' to: {}", editionId, targetFile);
                    return true;
                } else {
                    log.error("Failed to find or extract .mmdb file for '{}' from archive: {}", editionId, tempTarGz);
                    return false;
                }

            } catch (IOException | SecurityException e) {
                log.error("IO/Security error during download/extraction for '{}' to {}: {}", editionId, targetFile, e.getMessage(), e);
                return false;
            } finally {
                // Clean up temporary file
                if (tempTarGz != null) {
                    try {
                        Files.deleteIfExists(tempTarGz);
                    } catch (IOException e) {
                        log.warn("Failed to delete temporary download file: {}", tempTarGz, e);
                    }
                }
            }
        };

        // --- 3. Execute the Download using RestTemplate ---
        try {
            Boolean success = restTemplate.execute(downloadUrl, HttpMethod.GET, requestCallback, responseExtractor);
            // Check if file exists *after* execute returns, as execute might return null on some errors
            return Boolean.TRUE.equals(success) && Files.exists(targetFile);

        } catch (HttpClientErrorException e) {
            log.error("Client error downloading {} database: {} - {}", editionId, e.getStatusCode(), e.getMessage());
            String responseBody = e.getResponseBodyAsString();
            if (!responseBody.isEmpty()) {
                log.error("Error response body: {}", responseBody);
            }
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("Check your MaxMind Account ID and License Key.");
            }
            return false; // Return false on client error
        } catch (HttpServerErrorException e) {
            log.error("Server error downloading {} database: {} - {}", editionId, e.getStatusCode(), e.getMessage());
            return false; // Return false on server error
        } catch (RestClientException e) {
            // Catch other potential RestTemplate errors (network, etc.)
            log.error("Network or RestClient error downloading {} database: {}", editionId, e.getMessage());
            return false; // Return false on other rest client errors
        } catch (Exception e) {
            // Catch any other unexpected errors during the execute call
            log.error("Unexpected error during RestTemplate execute for {} database: {}", editionId, e.getMessage(), e);
            return false; // Return false on unexpected errors
        }
    }

    /**
     * Extracts the .mmdb file from a downloaded .tar.gz archive.
     * The MaxMind archives typically contain a directory (e.g., GeoLite2-City_YYYYMMDD)
     * and the .mmdb file inside it. This method searches for the .mmdb file
     * matching the {@code editionId} and extracts it to the {@code targetMmdbFile} path.
     *
     * @param tarGzFile The path to the downloaded .tar.gz archive.
     * @param targetMmdbFile The path where the extracted .mmdb file should be saved.
     * @param editionId The MaxMind edition ID (e.g., "GeoLite2-City") to identify the correct .mmdb file within the archive.
     * @return {@code true} if the .mmdb file was found and successfully extracted, {@code false} otherwise.
     * @throws IOException If an I/O error occurs during archive processing or file writing.
     */
    private boolean extractMmdbFromTarGz(Path tarGzFile, Path targetMmdbFile, String editionId) throws IOException {
        try (InputStream fi = Files.newInputStream(tarGzFile);
             BufferedInputStream bi = new BufferedInputStream(fi);
             GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = ti.getNextEntry()) != null) {
                // MaxMind archives usually contain a directory like GeoLite2-City_YYYYMMDD/
                // and the file inside is GeoLite2-City.mmdb
                log.debug("Found entry in archive: {}", entry.getName());
                if (!entry.isDirectory() && entry.getName().endsWith(editionId + ".mmdb")) {
                    log.info("Found target .mmdb file in archive: {}", entry.getName());
                    // Ensure parent directory exists for the target file
                    Files.createDirectories(targetMmdbFile.getParent());
                    // Extract the file content directly to the target path
                    // Use StandardOpenOption.CREATE to create if not exists, TRUNCATE_EXISTING to overwrite
                    try (OutputStream outputFileStream = Files.newOutputStream(targetMmdbFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        StreamUtils.copy(ti, outputFileStream); // Use Spring's StreamUtils
                    }
                    log.info("Successfully extracted {} to {}", entry.getName(), targetMmdbFile);
                    return true; // Found and extracted
                }
            }
        }
        log.warn("Could not find an entry ending with '{}.mmdb' in the archive: {}", editionId, tarGzFile);
        return false; // .mmdb file not found in archive
    }
}
