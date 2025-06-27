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

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CityResponse;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import me.amlu.authserver.security.model.GeoLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Service for performing GeoIP lookups using the MaxMind GeoLite2 City database.
 * This service handles database loading and gracefully manages lookup failures by returning
 * This service will be the single point of contact for all GeoIP lookups. It will encapsulate the DatabaseReader logic,
 * handle exceptions gracefully, and always return a valid GeoLocation object (either the real one or the UNKNOWN sentinel).
 * a {@link GeoLocation#unknown()} sentinel object.
 */
@Service
public class GeoLookupService {

    private static final Logger log = LoggerFactory.getLogger(GeoLookupService.class);

    @Value("${geoip2.city-database.path}")
    private String cityDatabasePath;

    private DatabaseReader cityDbReader;
    private final MeterRegistry meterRegistry;

    public GeoLookupService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        try {
            File cityDatabase = new File(cityDatabasePath);
            if (!cityDatabase.exists()) {
                log.error("GeoLite2 City database not found at path: {}. GeoIP lookups will be disabled.", cityDatabase.getAbsolutePath());
                return; // Service will operate in a disabled state
            }
            this.cityDbReader = new DatabaseReader.Builder(cityDatabase).build();
            log.info("GeoLookupService initialized successfully with database: {}", cityDatabase.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load GeoLite2 City database. GeoIP lookups will not function.", e);
            this.cityDbReader = null;
        }
    }

    /**
     * Looks up the geographic location for a given IP address.
     *
     * @param ipAddress The IP address string to look up.
     * @return A non-null {@link GeoLocation} object. Returns {@link GeoLocation#unknown()} if the IP is invalid,
     * the database is not available, or the address is not found in the database.
     */
    @Timed(value = "authserver.geo.lookup", description = "Time taken to lookup geo location for an IP")
    public GeoLocation lookupLocation(String ipAddress) {
        if (cityDbReader == null) {
            log.warn("GeoIP lookup skipped: Database reader is not available.");
            meterRegistry.counter("geo.lookup.errors", "reason", "db_unavailable").increment();
            return GeoLocation.unknown();
        }

        if (!StringUtils.hasText(ipAddress)) {
            log.debug("GeoIP lookup skipped: IP address is blank.");
            return GeoLocation.unknown();
        }

        try {
            InetAddress ip = InetAddress.getByName(ipAddress);
            CityResponse response = cityDbReader.city(ip);

            meterRegistry.counter("geo.lookup.success").increment();

            // Use the builder to construct the GeoLocation object
            // and populate all available fields from the CityResponse.
            return GeoLocation.builder()
                    .countryCode(response.getCountry() != null ? response.getCountry().getIsoCode() : null)
                    .countryName(response.getCountry() != null ? response.getCountry().getName() : null)
                    .city(response.getCity() != null ? response.getCity().getName() : null)
                    .postalCode(response.getPostal() != null ? response.getPostal().getCode() : null)
                    .latitude(response.getLocation() != null ? response.getLocation().getLatitude() : null)
                    .longitude(response.getLocation() != null ? response.getLocation().getLongitude() : null)
                    .timeZone(response.getLocation() != null ? response.getLocation().getTimeZone() : null)
                    .subdivisionName(response.getMostSpecificSubdivision() != null ? response.getMostSpecificSubdivision().getName() : null)
                    .subdivisionCode(response.getMostSpecificSubdivision() != null ? response.getMostSpecificSubdivision().getIsoCode() : null)
                    // ASN is not available in CityResponse, so it will default to UNKNOWN in the builder.
                    .build();

        } catch (AddressNotFoundException e) {
            log.trace("IP address not found in GeoIP database: {}", ipAddress);
            meterRegistry.counter("geo.lookup.notfound").increment();
            return GeoLocation.unknown();
        } catch (Exception e) {
            log.error("Error during GeoIP lookup for IP: {}", ipAddress, e);
            meterRegistry.counter("geo.lookup.errors", "reason", "lookup_exception").increment();
            return GeoLocation.unknown();
        }
    }

    @PreDestroy
    public void destroy() {
        if (this.cityDbReader != null) {
            try {
                this.cityDbReader.close();
                log.info("GeoLite2 City database reader closed.");
            } catch (IOException e) {
                log.error("Error closing GeoLite2 City database reader.", e);
            }
        }
    }
}
