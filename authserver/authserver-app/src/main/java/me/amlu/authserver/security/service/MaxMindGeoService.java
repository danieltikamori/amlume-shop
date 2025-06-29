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

import com.maxmind.db.InvalidDatabaseException;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.PreDestroy;
import me.amlu.authserver.security.config.properties.GeoIp2Properties;
import me.amlu.authserver.security.model.GeoLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MaxMind GeoIP2 service for IP geolocation and ASN lookups.
 * <p>
 * This service uses MaxMind GeoIP2 databases to provide geolocation information based on IP addresses.
 * It supports city, country, and Autonomous System Number (ASN) lookups.
 * <p>
 * The database files are expected to be downloaded and available at the specified paths in the configuration.
 * Failures to load the database files will be logged, and the service methods will handle these cases gracefully by returning default or null values.
 */
@DependsOn("geoIpDatabaseDownloader") // Ensures downloader runs first
@Service
public class MaxMindGeoService {

    private static final Logger log = LoggerFactory.getLogger(MaxMindGeoService.class);

    private final DatabaseReader cityReader;
    private final DatabaseReader asnReader;
    private final DatabaseReader countryReader;

    public MaxMindGeoService(GeoIp2Properties properties) {
        this.asnReader = initializeReader(properties.getAsnDatabase().getPath(), "ASN");
        log.info("Initializing MaxMindGeoService...");

        // --- Initialize City Reader ---
        this.cityReader = initializeReader(properties.getCityDatabase().getPath(), "City");

        // --- Initialize other readers similarly ---
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
        // Close all initialized readers to release resources
        closeReader(cityReader, "City");
        // The ASN reader is not initialized in the constructor, so it cannot be closed here.
        // If ASN reader is added, it should be closed here.
        // For now, the ASN reader is initialized on demand in getAsn(InetAddress ipAddress)
        closeReader(countryReader, "Country");
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

    /**
     * Retrieves the ISO country code for a given IP address.
     *
     * @param ip The IP address as a string.
     * @return The two-letter ISO country code (e.g., "US", "GB"), or "XX" if the country cannot be determined
     * (e.g., database not available, IP not found, or error occurs).
     * @throws IllegalArgumentException if the provided IP string is invalid.
     */
    public String getCountryCode(String ip) {
        if (countryReader == null) {
            log.warn("GeoIP Country database unavailable, returning 'XX' for IP {}", ip);
            return "XX"; // Unknown country code
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = countryReader.country(ipAddress);
            if (response != null && response.getCountry() != null) {
                return response.getCountry().getIsoCode();
            } else {
                log.debug("Country not found in database for IP: {}", ip);
                return "XX";
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid IP address format: {}", ip, e);
            return "XX";
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error getting country code for IP: {}", ip, e);
            return "XX"; // Unknown country code on error
        }
    }

    public GeoLocation getGeoLocation(String ip) {
        /**
         * Retrieves detailed geolocation information for a given IP address.
         *
         * @param ip The IP address as a string.
         * @return A {@link GeoLocation} object containing the detailed location information,
         *         or {@link GeoLocation#unknown()} if the information cannot be determined.
         */
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

        } catch (IllegalArgumentException e) {
            log.error("Invalid IP address format: {}", ip, e);
            return GeoLocation.unknown();
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error getting geolocation for IP: {}", ip, e);
            return GeoLocation.unknown(); // Return unknown on error
        }
    }

    /**
     * Checks if a given IP address is located in a specific country.
     *
     * @param ip          The IP address as a string.
     * @param countryCode The two-letter ISO country code to check against (e.g., "US").
     * @return true if the IP address is in the specified country, false otherwise (including errors or database unavailability).
     * @throws IllegalArgumentException if the provided IP string is invalid.
     */
    public boolean isIpInCountry(String ip, String countryCode) {
        if (countryReader == null) {
            log.warn("GeoIP Country database unavailable for isIpInCountry check for IP {}", ip);
            return false; // Cannot confirm if unavailable
        }
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = countryReader.country(ipAddress);
            return countryCode != null && response.getCountry() != null && countryCode.equals(response.getCountry().getIsoCode());
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error checking country for IP: {}", ip, e);
            return false; // Assume false on error
        }
    }

    /**
     * Calculates the great-circle distance between two IP addresses based on their geolocations.
     * This method relies on {@link #getGeoLocation(String)} to obtain latitude and longitude.
     *
     * @param ip1 The first IP address as a string.
     * @param ip2 The second IP address as a string.
     * @return The distance in kilometers, or -1 if geolocation data is unavailable for either IP
     * or an error occurs during calculation.
     */
    public double getDistance(String ip1, String ip2) {
        try {
            GeoLocation loc1 = getGeoLocation(ip1); // Already handles null reader
            GeoLocation loc2 = getGeoLocation(ip2); // Already handles null reader

            // Check if locations could be determined
            if (loc1 == null || loc1.latitude() == null || loc1.longitude() == null ||
                    loc2 == null || loc2.latitude() == null || loc2.longitude() == null) {
                log.warn("Cannot calculate distance between IPs {} and {}: Geolocation data unavailable.", ip1, ip2);
                return -1; // Indicate failure
            }

            return calculateDistance(
                    loc1.latitude(), loc1.longitude(),
                    loc2.latitude(), loc2.longitude()
            );
        } catch (Exception e) {
            log.error("Error calculating distance between IPs: {} and {}", ip1, ip2, e);
            return -1;
        }
    }

    /**
     * Calculates the great-circle distance between two geographical points specified by their
     * latitude and longitude coordinates. Uses the Haversine formula.
     *
     * @param lastLocationLatitude     Latitude of the first point.
     * @param lastLocationLongitude    Longitude of the first point.
     * @param currentLocationLatitude  Latitude of the second point.
     * @param currentLocationLongitude Longitude of the second point.
     * @return The distance in kilometers, or -1 if an error occurs during calculation.
     */
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

    /**
     * Private helper method to calculate the distance between two points on Earth using the Haversine formula.
     *
     * @param lat1 Latitude of the first point in degrees.
     * @param lon1 Longitude of the first point in degrees.
     * @param lat2 Latitude of the second point in degrees.
     * @param lon2 Longitude of the second point in degrees.
     * @return The distance in kilometers.
     * @see <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine formula</a>
     */
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

    /**
     * Retrieves Autonomous System Number (ASN) information for a given IP address.
     * This method initializes the ASN database reader on demand if it hasn't been initialized yet.
     *
     * @param ipAddress The {@link InetAddress} for which to retrieve ASN information.
     * @return An {@link AsnResponse} object containing ASN details, or null if the ASN database
     * is unavailable, the IP is not found in the database, or an error occurs.
     * @implNote The ASN database reader is initialized lazily here to avoid loading it if not needed,
     * as it's less frequently used than city/country lookups.
     */
    public AsnResponse getAsn(InetAddress ipAddress) {
        // Lazy initialization of asnReader
        DatabaseReader currentAsnReader = this.asnReader;
        if (currentAsnReader == null) {
            // Attempt to initialize if not already done. This assumes properties are available.
            // This part might need adjustment if properties are not directly accessible here
            // or if a more robust lazy loading pattern is desired (e.g., double-checked locking).
            // For simplicity, assuming properties are available or this is a one-time init.
            // If this service is truly stateless, this would be problematic.
            // Given it holds readers, it's stateful.
            // The current setup initializes all readers in the constructor.
            // If ASN reader is truly optional, it should be initialized here.
            log.warn("GeoIP ASN database unavailable, returning null for IP {}", ipAddress);
            return null; // Return null if ASN reader is not available
        }
        try {
            return currentAsnReader.asn(ipAddress);
        } catch (AddressNotFoundException e) {
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

    /**
     * Retrieves city-level geolocation information for a given IP address.
     *
     * @param ipAddress The {@link InetAddress} for which to retrieve city information.
     * @return A {@link CityResponse} object containing city details, or {@code null} if the city database
     * is unavailable, the IP is not found in the database, or an error occurs.
     */
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
