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
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.security.model.GeoLocation;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;

@Service
@Slf4j
public class GeoIp2ServiceImpl implements GeoIp2Service {
    private final DatabaseReader databaseReader;
    // or private final WebServiceClient webServiceClient;

    public GeoIp2ServiceImpl(DatabaseReader databaseReader) {
        this.databaseReader = databaseReader;
    }

    @Override
    public String lookupAsn(String ip) throws GeoIp2Exception {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            AsnResponse response = databaseReader.asn(ipAddress);
            return "AS" + response.getAutonomousSystemNumber();
        } catch (AddressNotFoundException e) {
            log.debug("IP address not found in database: {}", ip);
            return null;
        } catch (InvalidDatabaseException e) {
            log.error("Invalid GeoIP2 database", e);
            throw new GeoIp2Exception("Invalid GeoIP2 database", e);
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error reading from GeoIP2 database", e);
            throw new GeoIp2Exception("Error reading from GeoIP2 database", e);
        }
    }

    @Override
    public GeoLocation lookupLocation(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = databaseReader.city(ipAddress);
            
            return GeoLocation.builder()
                    .countryCode(response.getCountry().getIsoCode())
                    .city(response.getCity().getName())
                    .latitude(response.getLocation().getLatitude())
                    .longitude(response.getLocation().getLongitude())
                    .build();
        } catch (Exception e) {
            log.error("Failed to lookup location for IP: {}", ip, e);
            return null;
        }
    }
}

