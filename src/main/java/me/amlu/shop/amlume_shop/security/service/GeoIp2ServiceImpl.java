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
import me.amlu.shop.amlume_shop.exceptions.GeoIpLookupException;
import me.amlu.shop.amlume_shop.security.model.GeoLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

@Service
public class GeoIp2ServiceImpl implements GeoIp2Service {
    private final DatabaseReader databaseReader;

    private static final Logger log = LoggerFactory.getLogger(GeoIp2ServiceImpl.class);

    public GeoIp2ServiceImpl(DatabaseReader databaseReader) {
        this.databaseReader = databaseReader;
    }

    @Override
    public AsnResponse lookupAsn(String ip) throws GeoIpLookupException { // Throw a custom wrapper exception
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            // databaseReader.asn can throw AddressNotFoundException, GeoIp2Exception, IOException
            return databaseReader.asn(ipAddress);
        } catch (UnknownHostException e) {
            log.debug("Invalid IP address format for ASN lookup: {}", ip);
            return null; // Treat invalid format as not found
        } catch (AddressNotFoundException e) {
            log.debug("ASN not found in database for IP: {}", ip);
            return null; // Expected case: IP valid but not in DB
        } catch (InvalidDatabaseException e) {
            log.error("Invalid GeoIP2 ASN database configured.", e);
            // This is a configuration/setup error, wrap and rethrow
            throw new GeoIpLookupException("Invalid GeoIP2 ASN database configuration", e);
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error looking up ASN for IP {}: {}", ip, e.getMessage());
            // This indicates an issue reading the DB or other GeoIP error
            throw new GeoIpLookupException("Failed to lookup ASN due to GeoIP2/IO error", e);
        }
    }

    // REMOVED lookupAsnString method - caller should format the result from lookupAsn

    @Override
    public Optional<GeoLocation> lookupLocation(String ip) throws GeoIpLookupException { // Return Optional and throw for serious errors
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            // databaseReader.city can throw AddressNotFoundException, GeoIp2Exception, IOException
            CityResponse response = databaseReader.city(ipAddress);

            // Build GeoLocation only if response is valid
            if (response != null && response.getLocation() != null) {
                return Optional.of(GeoLocation.builder()
                        .countryCode(response.getCountry() != null ? response.getCountry().getIsoCode() : null)
                        .city(response.getCity() != null ? response.getCity().getName() : null)
                        .latitude(response.getLocation().getLatitude())
                        .longitude(response.getLocation().getLongitude())
                        .build());
            } else {
                // Should typically be caught by AddressNotFoundException, but handle defensively
                log.debug("GeoIP2 city lookup returned null response or location for IP: {}", ip);
                return Optional.empty();
            }
        } catch (UnknownHostException e) {
            log.debug("Invalid IP address format for location lookup: {}", ip);
            return Optional.empty(); // Treat invalid format as not found
        } catch (AddressNotFoundException e) {
            log.debug("Location not found in database for IP: {}", ip);
            return Optional.empty(); // Expected case: IP valid but not in DB
        } catch (InvalidDatabaseException e) {
            log.error("Invalid GeoIP2 City database configured.", e);
            // This is a configuration/setup error, wrap and rethrow
            throw new GeoIpLookupException("Invalid GeoIP2 City database configuration", e);
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error looking up location for IP {}: {}", ip, e.getMessage());
            // This indicates an issue reading the DB or other GeoIP error
            throw new GeoIpLookupException("Failed to lookup location due to GeoIP2/IO error", e);
        }
    }
}

