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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service to update the GeoIP2 database from MaxMind.
 * <p>
 * This service downloads the latest GeoIP2 databases (City, ASN, and Country) from MaxMind's servers
 * and extracts them to a specified directory. It runs on a scheduled basis (every Wednesday at midnight).
 * <p>
 * Database	Update Schedule
 * GeoIP2 Country	Every Tuesday and Friday.
 * GeoIP2 City	Every Tuesday and Friday.
 * GeoIP2 Connection Type	Every Tuesday and Friday.
 * GeoIP2 ISP	Every Tuesday and Friday.
 * GeoIP2 Domain Name	Every Tuesday and Friday.
 * GeoIP2 Anonymous IP	Every day.
 * GeoIP2 Enterprise	Every Tuesday and Friday.
 * GeoLite2 Country	Every Tuesday and Friday.
 * GeoLite2 City	Every Tuesday and Friday.
 * GeoLite2 ASN	Every day.
 */


@Service
public class GeoIpDatabaseUpdater {

    private static final Logger log = LoggerFactory.getLogger(GeoIpDatabaseUpdater.class);

//    //    // accountId is not needed
//    @Value("${geoip2.license.account-id}")
//    private String accountId;

    @Value("${geoip2.license.license-key}")
    private String licenseKey;

    @Value("${geoip2.download-path}")
    private String downloadPath;

    @Value("${geoip2.database-directory}")
    private String databaseDirectory;

    /**
     * Scheduled task to update the GeoIP2 database.
     * This method is scheduled to run every Wednesday at midnight.
     * It downloads the latest databases from MaxMind and extracts them to the specified directory.
     * <p>
     * Considerations:
     * - Ensure that the database directory is writable by the application.
     * - Handle exceptions appropriately to avoid application crashes.
     * - Consider logging the success or failure of the update process.
     * - Ensure that the database files are not in use while being updated.
     * - Consider implementing a backup strategy for the existing database files before updating.
     * - Ensure that the application has internet access to download the databases.
     * - Consider using a more robust error handling strategy, such as retrying the download on failure.
     * - Consider using a library for downloading files that can handle large files and resume downloads.
     * - Consider using a library for extracting tar.gz files that can handle large files and errors.
     * * Note: The cron expression "0 0 0 * * WED" means "At 00:00 (midnight) on Wednesday".
     * * @throws IOException        If an I/O error occurs during the download or extraction.
     * * @throws URISyntaxException If the URL is malformed.
     * * @throws InterruptedException If the thread is interrupted during sleep.
     */
    @Scheduled(cron = "0 0 0 * * WED") // Run every Wednesday at midnight
    public void updateDatabase() {
        log.info("Starting GeoIP2 database update at {}", Instant.now());
        log.info("Database path: {}", databaseDirectory);

        try {
            // For City database
            String url = String.format(
                    "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=%s&suffix=tar.gz",
                    licenseKey
            );

            // For ASN database
            String asnUrl = String.format(
                    "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-ASN&license_key=%s&suffix=tar.gz",
                    licenseKey
            );

            // For Country database
            String countryUrl = String.format(
                    "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=%s&suffix=tar.gz",
                    licenseKey
            );

            Path databaseDirectoryPath = Paths.get(databaseDirectory);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String timestamp = LocalDateTime.now().format(formatter);

            String[] databaseFileNames = {"GeoLite2-City.mmdb", "GeoLite2-ASN.mmdb", "GeoLite2-Country.mmdb"};

            for (String fileName : databaseFileNames) {
                Path sourcePath = databaseDirectoryPath.resolve(fileName);
                Path backupDirectory = databaseDirectoryPath.resolve("backup");
                Path backupPath = backupDirectory.resolve(fileName + "_" + timestamp);
                try {
                    Files.createDirectories(backupDirectory);
                    if (Files.exists(sourcePath)) {
                        Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Backed up {} to {}", sourcePath, backupPath);
                    }
                } catch (IOException e) {
                    log.error("Error creating backup of {}", sourcePath, e);
                }
            }

            // Download and extract new database
            downloadAndUpdateDatabase(url);
            downloadAndUpdateDatabase(asnUrl);
            log.info("GeoIP2 database updated successfully");
        } catch (Exception e) {
            log.error("Failed to update GeoIP2 database through Maxmind, trying to download from fallback", e);
            // Attempt to download from fallback URL
            try {
                // Fallback URL for City database
                String cityUrlFallback = "https://git.io/GeoLite2-City.mmdb";
                downloadMmdbFile(cityUrlFallback, "GeoLite2-City.mmdb");
                log.info("GeoIP2 City database updated successfully from fallback URL");

                // Fallback URL for ASN database
                String asnUrlFallback = "https://git.io/GeoLite2-ASN.mmdb";
                downloadMmdbFile(asnUrlFallback, "GeoLite2-ASN.mmdb");
                log.info("GeoIP2 ASN database updated successfully from fallback URL");

                // Fallback URL for Country database
                String countryUrlFallback = "https://git.io/GeoLite2-Country.mmdb";
                downloadMmdbFile(countryUrlFallback, "GeoLite2-Country.mmdb");
                log.info("GeoIP2 Country database updated successfully from fallback URL");

            } catch (Exception fallbackException) {
                log.error("Failed to update GeoIP2 database from fallback URL", fallbackException);
            }
        }
    }

    /**
     * Downloads the GeoIP2 database from the specified URL and extracts it to the database directory.
     *
     * @param url The URL to download the GeoIP2 database from.
     * @throws IOException        If an I/O error occurs during the download or extraction.
     * @throws URISyntaxException If the URL is malformed.
     */

    private void downloadAndUpdateDatabase(String url) throws IOException, URISyntaxException {

        int maxRetries = 3;
        int retryDelaySeconds = 10;

        // Retry downloading the database up to maxRetries times
        for (int i = 0; i < maxRetries; i++) {
            Path downloadDirectory = Paths.get(this.downloadPath);
            Files.createDirectories(downloadDirectory); // Ensure the directory exists
            String filename = url.substring(url.lastIndexOf('/') + 1);
            Path tempFile = downloadDirectory.resolve(filename);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);

                HttpClientResponseHandler<Boolean> responseHandler = response -> {
                    final int status = response.getCode();
                    if (status >= 200 && status < 300) {
                        final HttpEntity entity = response.getEntity();

                        if (entity != null) {
                            try (InputStream in = entity.getContent()) {
                                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                                String lastTenChars = url.length() > 10 ? url.substring(url.length() - 10) : url;
                                log.info("Successfully downloaded GeoIP2 database from link ending with: {} using Apache HttpClient 5.", lastTenChars);
                                log.info("Downloaded GeoIP2 database to: {}", tempFile);
                                return true;
                            }
                        } else {
                            log.warn("Empty content received from {}", url);
                            return false;
                        }
                    } else {
                        log.error("Received unexpected status code {} from {}", status, url);
                        return false;
                    }
                };

                boolean success = httpClient.execute(httpGet, responseHandler);
                if (success) {
                    String lastTenChars = url.length() > 10 ? url.substring(url.length() - 10) : url;
                    extractDatabase(tempFile);
                    log.info("Successfully extracted GeoIP2 database from link ending with: : {}", lastTenChars);
                    log.info("Successfully downloaded after {} attempts.", i + 1);
                } else {
                    throw new IOException("Failed to download or process the response from " + url);
                }
            } catch (IOException e) {
                log.error("Error downloading from {} (attempt {}/{}), retrying in {} seconds: {}",
                        url, i + 1, maxRetries, retryDelaySeconds, e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelaySeconds * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Download retry interrupted.");
                        throw new IOException("Download interrupted after " + (i + 1) + " attempts.", ie);
                    }
                } else {
                    throw new IOException("Failed to download from " + url + " after " + maxRetries + " attempts.", e);
                }
            } finally {
                Files.deleteIfExists(tempFile);
                log.info("Temporary file deleted: {}, path: {} at time: {}", tempFile, tempFile.toAbsolutePath(), Instant.now());
            }
        }
    }

    /**
     * Downloads a file from the specified URL and saves it to the specified destination.
     *
     * @param url The URL to download the file from.
     * @throws IOException If an I/O error occurs during the download.
     */
    private void downloadMmdbFile(String url, String destinationFileName) throws IOException {
        int maxRetries = 3;
        int retryDelaySeconds = 10;
        Path destinationPath = Paths.get(databaseDirectory, destinationFileName);
        Path tempFile = Paths.get(downloadPath, destinationFileName + ".tmp"); // Use a temp file

        for (int i = 0; i < maxRetries; i++) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);
                HttpClientResponseHandler<Path> responseHandler = response -> {
                    final int status = response.getCode();
                    if (status >= 200 && status < 300) {
                        final HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            try (InputStream in = entity.getContent()) {
                                Files.createDirectories(destinationPath.getParent());
                                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                                log.info("Successfully downloaded {} to {}", url, tempFile);
                                return tempFile;
                            }
                        } else {
                            log.warn("Empty content received from {}", url);
                            return null;
                        }
                    } else {
                        log.error("Received unexpected status code {} from {}", status, url);
                        return null;
                    }
                };
                Path downloadedFile = httpClient.execute(httpGet, responseHandler);
                if (downloadedFile != null) {
                    Files.move(downloadedFile, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Moved downloaded file to {}", destinationPath);
                    return; // Success
                }
            } catch (IOException e) {
                log.error("Error downloading {} (attempt {}/{}), retrying...", url, i + 1, maxRetries, e);
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelaySeconds * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Download interrupted.");
                        Thread.currentThread().interrupt();
                        throw new IOException("Download interrupted", ie);
                    }
                }
            }
        }
        throw new IOException("Failed to download " + url + " after " + maxRetries + " attempts.");
    }

    private void extractDatabase(Path tarGzFile) throws IOException {
        // Implementation of tar.gz extraction
        Path localDatabasePath = tarGzFile.getParent();
        try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarGzFile.toFile())))) {
            log.info("Extracting database from: {}", tarGzFile);
            TarArchiveEntry entry;
            while ((entry = tarArchiveInputStream.getNextEntry()) != null) {
                Path filePath = localDatabasePath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    createDirectory(filePath);
                    continue;
                }
                copyFile(tarArchiveInputStream, filePath);
                if (filePath.getFileName().toString().endsWith(".mmdb")) {
                    log.info("Extracted GeoIP2 database: {}", filePath);
                    Path destinationPath = Paths.get(databaseDirectory, filePath.getFileName().toString());
                    try {
                        Files.createDirectories(destinationPath.getParent()); // Ensure directory exists
                        Files.move(filePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Moved extracted database to: {}", destinationPath);
                    } catch (IOException e) {
                        log.error("Error moving extracted database to destination", e);
                    }
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
    }
}
