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

import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.AbstractNamedRecord;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.Subdivision;
import io.micrometer.core.annotation.Timed;
import me.amlu.authserver.security.model.GeoLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Service implementation for performing GeoIP2 lookups using MaxMind's GeoIP2 database.
 * This service provides methods to lookup Autonomous System Number (ASN) and geographical location
 * based on an IP address.
 *
 * <p>This service integrates with {@link MaxMindGeoService} to perform the actual database queries.
 * It handles IP address parsing and provides a structured {@link GeoLocation} object for location lookups.</p>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * @Autowired
 * private GeoIp2Service geoIp2Service;
 *
 * public void processIpAddress(String ip) {
 *     Optional<GeoLocation> location = geoIp2Service.lookupLocation(ip);
 *     location.ifPresent(loc -> System.out.println("City: " + loc.getCity()));
 *     AsnResponse asn = geoIp2Service.lookupAsn(ip);
 *     if (asn != null) System.out.println("ASN: " + asn.getAutonomousSystemOrganization());
 * }
 * }</pre>
 */
@Service
public class GeoIp2ServiceImpl implements GeoIp2Service {
    private final MaxMindGeoService maxMindGeoService;
    private static final Logger log = LoggerFactory.getLogger(GeoIp2ServiceImpl.class);

    public GeoIp2ServiceImpl(MaxMindGeoService maxMindGeoService) {
        this.maxMindGeoService = maxMindGeoService;
    }

    @Override
    /**
     * Performs an Autonomous System Number (ASN) lookup for a given IP address.
     *
     * @param ip The IP address string for which to perform the ASN lookup.
     * @return An {@link AsnResponse} object if the lookup is successful, or {@code null} if the IP is invalid or lookup fails.
     */
    @Timed(value = "authserver.geolocation.lookup-asn", description = "Time taken to lookup ASN")
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
    /**
     * Performs a geographical location lookup for a given IP address.
     *
     * @param ip The IP address string for which to perform the location lookup.
     * @return An {@link Optional} containing a {@link GeoLocation} object if the lookup is successful, or {@link Optional#empty()} if the IP is invalid or lookup fails.
     */
    @Timed(value = "authserver.geolocation.lookup-location", description = "Time taken to lookup location")
    public Optional<GeoLocation> lookupLocation(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            // maxMindGeoService.getCity handles its own exceptions and returns null on failure
            CityResponse response = maxMindGeoService.getCity(ipAddress);

            // Build GeoLocation only if response is valid
            if (response != null && response.getLocation() != null) {
                // Ensure all necessary fields are checked for null before accessing
                String countryCode = Optional.ofNullable(response.getCountry()).map(Country::getIsoCode).orElse(null);
                String cityName = Optional.ofNullable(response.getCity()).map(AbstractNamedRecord::getName).orElse(null);
                Double latitude = response.getLocation().getLatitude();
                Double longitude = response.getLocation().getLongitude();

                // Add other fields as needed, checking for nulls
                String countryName = Optional.ofNullable(response.getCountry()).map(AbstractNamedRecord::getName).orElse(null);
                String postalCode = Optional.ofNullable(response.getPostal()).map(Postal::getCode).orElse(null);
                String timeZone = response.getLocation().getTimeZone();
                String subdivisionName = Optional.ofNullable(response.getMostSpecificSubdivision()).map(AbstractNamedRecord::getName).orElse(null);
                String subdivisionCode = Optional.ofNullable(response.getMostSpecificSubdivision()).map(Subdivision::getIsoCode).orElse(null);
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
