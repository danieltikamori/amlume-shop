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

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import me.amlu.shop.amlume_shop.security.model.GeoLocation;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Service to interact with MaxMind's GeoIP2 database.
 * This service provides methods to get country codes, geolocation data,
 * check if an IP is in a specific country, and calculate distances between two IPs.
 * <p>
 * This service uses the MaxMind GeoIP2 Java API to read the database files.
 * It requires the database files to be present in the specified path.
 * <p>
 * The database files can be downloaded from MaxMind's website.
 * Make sure to keep the database files updated regularly.
 * <p>
 * TODO: Check if it is necessary to use several databases (ASN, City, Country).
 */
@Service
public class MaxMindGeoService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MaxMindGeoService.class);

    @Value("${geoip2.city-database.path}")
    private Resource geoipDatabase;

    private DatabaseReader reader;

    @PostConstruct
    public void init() throws IOException {
        File database = geoipDatabase.getFile();
        reader = new DatabaseReader.Builder(database).build();
    }

    @PreDestroy
    public void cleanup() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    public String getCountryCode(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = reader.country(ipAddress);
            return response.getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error getting country code for IP: {}", ip, e);
            return "XX"; // Unknown country code
        }
    }


    public GeoLocation getGeoLocation(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);

            return GeoLocation.builder()
                    .countryCode(response.getCountry().getIsoCode())
                    .countryName(response.getCountry().getName())
                    .city(response.getCity().getName())
                    .postalCode(response.getPostal().getCode())
                    .latitude(response.getLocation().getLatitude())
                    .longitude(response.getLocation().getLongitude())
                    .timeZone(response.getLocation().getTimeZone())
                    .subdivisionName(response.getMostSpecificSubdivision().getName())
                    .subdivisionCode(response.getMostSpecificSubdivision().getIsoCode())
                    .build();

        } catch (IOException | GeoIp2Exception e) {
            log.error("Error getting geolocation for IP: {}", ip, e);
            return GeoLocation.unknown();
        }
    }

    public boolean isIpInCountry(String ip, String countryCode) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);
            return countryCode.equals(response.getCountry().getIsoCode());
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error checking country for IP: {}", ip, e);
            return false;
        }
    }

    public double getDistance(String ip1, String ip2) {
        try {
            GeoLocation loc1 = getGeoLocation(ip1);
            GeoLocation loc2 = getGeoLocation(ip2);

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
        // Haversine formula
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

//    public GeoLocation getCountryAndContinentGeoLocation(String ip) {
//        try {
//            InetAddress ipAddress = InetAddress.getByName(ip);
//            CountryResponse response = reader.country(ipAddress);
//
//            return GeoLocation.builder()
//                    .countryCode(response.getCountry().getIsoCode())
//                    .countryName(response.getCountry().getName())
//                    .continent(response.getContinent().getName())
//                    .build();
//        } catch (IOException | GeoIp2Exception e) {
//            log.error("Error getting geolocation for IP: {}", ip, e);
//            return GeoLocation.unknown();
//        }
//    }
}

