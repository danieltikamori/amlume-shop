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

import com.maxmind.db.InvalidDatabaseException;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.PreDestroy;
import me.amlu.shop.amlume_shop.config.properties.GeoIp2Properties;
import me.amlu.shop.amlume_shop.security.model.GeoLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
// Removed @Value and Resource imports as path comes from properties now
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MaxMind GeoIP2 service for IP geolocation and ASN lookups.
 * <p>
 * This service uses the MaxMind GeoIP2 database to provide geolocation information based on IP addresses.
 * It supports city, country, and ASN lookups.
 * <p>
 * The database files are expected to be downloaded and available at the specified paths in the configuration.
 * Failures to load the database files will be logged, and the service will handle these cases gracefully.
 */
@DependsOn("geoIpDatabaseDownloader") // Ensures downloader runs first
@Service
public class MaxMindGeoService {

    private static final Logger log = LoggerFactory.getLogger(MaxMindGeoService.class);

    // Make readers final but initialize conditionally
    private final DatabaseReader cityReader;
    // Add other readers if used (ASN, Country)
     private final DatabaseReader asnReader;
     private final DatabaseReader countryReader;

    // Constructor now handles potential missing files gracefully
    public MaxMindGeoService(GeoIp2Properties properties) {
        log.info("Initializing MaxMindGeoService...");

        // --- Initialize City Reader ---
        this.cityReader = initializeReader(properties.getCityDatabase().getPath(), "City");

        // --- Initialize other readers similarly ---
         this.asnReader = initializeReader(properties.getAsnDatabase().getPath(), "ASN");
         this.countryReader = initializeReader(properties.getCountryDatabase().getPath(), "Country");

        log.info("MaxMindGeoService initialization complete.");
    }

    /**
     * Helper method to initialize a DatabaseReader safely.
     *
     * @param pathStr Path to the database file from properties.
     * @param dbType  Type of database for logging (e.g., "City", "ASN").
     * @return Initialized DatabaseReader or null if initialization fails.
     */
    private DatabaseReader initializeReader(String pathStr, String dbType) {
        Path dbPath = null;
        try {
            dbPath = Paths.get(pathStr);
            if (Files.exists(dbPath) && Files.isReadable(dbPath)) {
                DatabaseReader reader = new DatabaseReader.Builder(dbPath.toFile()).build();
                log.info("Successfully loaded GeoIP2 {} database from: {}", dbType, dbPath);
                return reader;
            } else {
                // File doesn't exist or isn't readable
                log.error("GeoIP2 {} database file not found or not readable at: {}. {} lookups will be unavailable.", dbType, dbPath, dbType);
                return null; // Indicate failure to load
            }
        } catch (IOException e) {
            log.error("IOException initializing GeoIP2 {} database reader from {}: {}", dbType, dbPath, e.getMessage());
            return null; // Indicate failure to load
        } catch (Exception e) {
            // Catch InvalidPathException or other errors during Paths.get or Builder
            log.error("Unexpected error initializing GeoIP2 {} database reader for path '{}': {}", dbType, pathStr, e.getMessage());
            return null; // Indicate failure to load
        }
    }

    // Removed @PostConstruct init() method as initialization is in constructor

    @PreDestroy
    public void cleanup() {
        closeReader(cityReader, "City");
        // closeReader(asnReader, "ASN");
        // closeReader(countryReader, "Country");
    }

    private void closeReader(DatabaseReader reader, String dbType) {
        if (reader != null) {
            try {
                reader.close();
                log.info("Closed GeoIP2 {} database reader.", dbType);
            } catch (IOException e) {
                log.warn("Error closing GeoIP2 {} database reader: {}", dbType, e.getMessage());
            }
        }
    }

    // --- Service Methods Now Check for Null Readers ---

    public String getCountryCode(String ip) {
        // Assuming Country lookups use the City reader or a dedicated countryReader
        DatabaseReader readerToCheck = cityReader; // Or countryReader if you have one
        if (readerToCheck == null) {
            log.warn("GeoIP database unavailable for Country lookup for IP {}", ip);
            return "XX"; // Unknown country code
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            // Use the appropriate reader method (country() or city())
            CountryResponse response = readerToCheck.country(ipAddress);
            return response.getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error getting country code for IP: {}", ip, e);
            return "XX"; // Unknown country code on error
        }
    }

    public GeoLocation getGeoLocation(String ip) {
        if (this.cityReader == null) {
            log.warn("GeoIP City database unavailable, returning unknown location for IP {}", ip);
            return GeoLocation.unknown();
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = cityReader.city(ipAddress); // Use the initialized reader

            // Check for null response components before accessing them
            String countryCode = (response.getCountry() != null) ? response.getCountry().getIsoCode() : null;
            String countryName = (response.getCountry() != null) ? response.getCountry().getName() : null;
            String cityName = (response.getCity() != null) ? response.getCity().getName() : null;
            String postalCode = (response.getPostal() != null) ? response.getPostal().getCode() : null;
            Double latitude = (response.getLocation() != null) ? response.getLocation().getLatitude() : null;
            Double longitude = (response.getLocation() != null) ? response.getLocation().getLongitude() : null;
            String timeZone = (response.getLocation() != null) ? response.getLocation().getTimeZone() : null;
            String subdivisionName = (response.getMostSpecificSubdivision() != null) ? response.getMostSpecificSubdivision().getName() : null;
            String subdivisionCode = (response.getMostSpecificSubdivision() != null) ? response.getMostSpecificSubdivision().getIsoCode() : null;

            return GeoLocation.builder()
                    .countryCode(countryCode)
                    .countryName(countryName)
                    .city(cityName)
                    .postalCode(postalCode)
                    .latitude(latitude)
                    .longitude(longitude)
                    .timeZone(timeZone)
                    .subdivisionName(subdivisionName)
                    .subdivisionCode(subdivisionCode)
                    .build();

        } catch (IOException | GeoIp2Exception e) {
            log.error("Error getting geolocation for IP: {}", ip, e);
            return GeoLocation.unknown(); // Return unknown on error
        }
    }

    public boolean isIpInCountry(String ip, String countryCode) {
        // Assuming Country lookups use the City reader or a dedicated countryReader
        DatabaseReader readerToCheck = cityReader; // Or countryReader
        if (readerToCheck == null) {
            log.warn("GeoIP database unavailable for isIpInCountry check for IP {}", ip);
            return false; // Cannot confirm if unavailable
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            // Use the appropriate reader method
            CountryResponse response = readerToCheck.country(ipAddress);
            return countryCode != null && response.getCountry() != null && countryCode.equals(response.getCountry().getIsoCode());
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error checking country for IP: {}", ip, e);
            return false; // Assume false on error
        }
    }

    // getDistance methods remain the same as they rely on getGeoLocation which now handles null readers
    public double getDistance(String ip1, String ip2) {
        try {
            GeoLocation loc1 = getGeoLocation(ip1); // Already handles null reader
            GeoLocation loc2 = getGeoLocation(ip2); // Already handles null reader

            // Check if locations could be determined
            if (loc1 == null || loc1.getLatitude() == null || loc1.getLongitude() == null ||
                    loc2 == null || loc2.getLatitude() == null || loc2.getLongitude() == null) {
                log.warn("Cannot calculate distance between IPs {} and {}: Geolocation data unavailable.", ip1, ip2);
                return -1; // Indicate failure
            }

            return calculateDistance(
                    loc1.getLatitude(), loc1.getLongitude(),
                    loc2.getLatitude(), loc2.getLongitude()
            );
        } catch (Exception e) {
            log.error("Error calculating distance between IPs: {} and {}", ip1, ip2, e);
            return -1;
        }
    }

    public double getDistance(double lastLocationLatitude, double lastLocationLongitude, double currentLocationLatitude, double currentLocationLongitude) {
        try {
            return calculateDistance(
                    lastLocationLatitude, lastLocationLongitude,
                    currentLocationLatitude, currentLocationLongitude
            );
        } catch (Exception e) {
            String lastLocationCoordinates = String.format("lat: %s, lon: %s", lastLocationLatitude, lastLocationLongitude);
            String currentLocationCoordinates = String.format("lat: %s, lon: %s", currentLocationLatitude, currentLocationLongitude);
            log.error("Error calculating distance between current and last location: {} and {}", currentLocationCoordinates, lastLocationCoordinates, e);
            return -1;
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula (implementation remains the same)
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public AsnResponse getAsn(InetAddress ipAddress) {
        if (this.asnReader == null) {
            log.warn("GeoIP ASN database unavailable, returning null for IP {}", ipAddress);
            return null; // Return null if ASN reader is not available
        }
        try {
            return this.asnReader.asn(ipAddress);
        }
        catch (AddressNotFoundException e) {
            log.debug("ASN not found in database for IP: {}", ipAddress);
            return null; // Expected case: IP valid but not in DB
        } catch (InvalidDatabaseException e) {
            log.error("Invalid GeoIP2 ASN database configured.", e);
            return null; // Return null on invalid database
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error getting ASN for IP: {}", ipAddress, e);
            return null; // Return null on error
        }
    }

    public CityResponse getCity(InetAddress ipAddress) {
        if (this.cityReader == null) {
            log.warn("GeoIP City database unavailable, returning null for IP {}", ipAddress);
            return null; // Return null if city reader is not available
        }
        try {
            return this.cityReader.city(ipAddress);
        } catch (AddressNotFoundException e) {
            log.debug("City not found in database for IP: {}", ipAddress);
            return null; // Expected case: IP valid but not in DB
        } catch (InvalidDatabaseException e) {
            log.error("Invalid GeoIP2 City database configured.", e);
            return null; // Return null on invalid database
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error getting city for IP: {}", ipAddress, e);
            return null; // Return null on error
        }
    }
}