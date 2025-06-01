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

import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import io.micrometer.core.annotation.Timed;
import me.amlu.shop.amlume_shop.security.model.GeoLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

@Service
public class GeoIp2ServiceImpl implements GeoIp2Service {
    private final MaxMindGeoService maxMindGeoService;

    private static final Logger log = LoggerFactory.getLogger(GeoIp2ServiceImpl.class);

    public GeoIp2ServiceImpl(MaxMindGeoService maxMindGeoService) {
        this.maxMindGeoService = maxMindGeoService;
    }

    @Override
    @Timed(value = "shopapp.geolocation.lookup-asn", description = "Time taken to lookup ASN")
    public AsnResponse lookupAsn(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            // maxMindGeoService.getAsn handles its own exceptions and returns null on failure
            return maxMindGeoService.getAsn(ipAddress);
        } catch (UnknownHostException e) {
            log.debug("Invalid IP address format for ASN lookup: {}", ip);
            return null; // Treat invalid format as not found
        }
    }

    @Override
    @Timed(value = "shopapp.geolocation.lookup-location", description = "Time taken to lookup location")
    public Optional<GeoLocation> lookupLocation(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            // maxMindGeoService.getCity handles its own exceptions and returns null on failure
            CityResponse response = maxMindGeoService.getCity(ipAddress);

            // Build GeoLocation only if response is valid
            if (response != null && response.getLocation() != null) {
                // Ensure all necessary fields are checked for null before accessing
                String countryCode = (response.getCountry() != null) ? response.getCountry().getIsoCode() : null;
                String cityName = (response.getCity() != null) ? response.getCity().getName() : null;
                Double latitude = response.getLocation().getLatitude(); // Assuming getLocation() is checked above
                Double longitude = response.getLocation().getLongitude(); // Assuming getLocation() is checked above

                // Add other fields as needed, checking for nulls
                String countryName = (response.getCountry() != null) ? response.getCountry().getName() : null;
                String postalCode = (response.getPostal() != null) ? response.getPostal().getCode() : null;
                String timeZone = response.getLocation().getTimeZone(); // Assuming getLocation() is checked above
                String subdivisionName = (response.getMostSpecificSubdivision() != null) ? response.getMostSpecificSubdivision().getName() : null;
                String subdivisionCode = (response.getMostSpecificSubdivision() != null) ? response.getMostSpecificSubdivision().getIsoCode() : null;
                // ASN might not be available from CityResponse, depends on MaxMindGeoService.getCity implementation
                // String asn = ...; // Get ASN if available

                return Optional.of(GeoLocation.builder()
                        .countryCode(countryCode)
                        .countryName(countryName)
                        .city(cityName)
                        .postalCode(postalCode)
                        .latitude(latitude)
                        .longitude(longitude)
                        .timeZone(timeZone)
                        .subdivisionName(subdivisionName)
                        .subdivisionCode(subdivisionCode)
                        // .asn(asn) // Add if available
                        .build());
            } else {
                log.debug("GeoIP2 city lookup returned null response or location for IP: {}", ip);
                return Optional.empty();
            }
        } catch (UnknownHostException e) {
            log.debug("Invalid IP address format for location lookup: {}", ip);
            return Optional.empty(); // Treat invalid format as not found
        }
        // No need for a catch block for GeoIp2Exception/IOException here,
        // as MaxMindGeoService handles them internally.
    }
}
