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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.maxmind.geoip2.model.AsnResponse;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AsnLookupServiceImpl implements AsnLookupService {

    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 requests per second
    private final GeoIp2Service geoIp2Service;
    private final LoadingCache<String, String> asnCache;

    @Value("${web.whois.server}")
    private String whoisServer;

    public AsnLookupServiceImpl(GeoIp2Service geoIp2Service) {
        this.geoIp2Service = geoIp2Service;
        this.asnCache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build(new CacheLoader<String, String>() {
                    @NotNull
                    @Override
                    public String load(@NotNull String ip) {
                        return lookupAsnUncached(ip);
                    }
                });
    }

    /**
     * Lookups the Autonomous System Number (ASN) for a given IP address.
     * This method will first try to lookup the ASN using DNS reverse lookup,
     * and if that fails, it will fall back to WHOIS lookup.
     * The method is rate-limited to prevent abuse and ensure fair usage.
     * If the rate limit is exceeded, a RateLimitExceededException is thrown.
     * The method returns null if the lookup fails.
     *
     * @param ip the IP address to lookup
     * @return the ASN for the given IP address, or null if the lookup fails
     * @throws RateLimitExceededException if the rate limit for ASN lookups is exceeded
     */
    @Override
    public String lookupAsn(String ip) {
        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded for ASN lookups");
            throw new RateLimitExceededException("Too many ASN lookup requests");
        }

        try {
            return asnCache.get(ip);
        } catch (ExecutionException e) {
            log.error("Failed to lookup ASN for IP: {}", ip, e);
            return null;
        }
    }

    //    private String lookupAsn(String ip) {
//        try {
//            // First try DNS reverse lookup for ASN
//            String asn = lookupAsnViaDns(ip);
//            if (asn != null) {
//                return asn;
//            }
//
//            // Fallback to WHOIS lookup
//            return lookupAsnViaWhois(ip);
//        } catch (Exception e) {
//            log.error("Failed to lookup ASN for IP: " + ip, e);
//            return null;
//        }
//    }

    /**
     * Performs an ASN lookup for the given IP address using the MaxMind GeoIP2 database.
     * The method is rate-limited to prevent abuse and ensure fair usage.
     * If the rate limit is exceeded, a RateLimitExceededException is thrown.
     * The method returns null if the lookup fails.
     *
     * @param ip the IP address to lookup
     * @return the ASN for the given IP address, or null if the lookup fails
     * @throws RateLimitExceededException if the rate limit for ASN lookups is exceeded
     */
    @Override
    public String lookupAsnWithGeoIp2(String ip) {
        if (!rateLimiter.tryAcquire()) {
            AsnLookupServiceImpl.log.warn("Rate limit exceeded for ASN lookups");
            throw new RateLimitExceededException("Too many ASN lookup requests");
        }

        try {
            AsnResponse response = geoIp2Service.lookupAsn(String.valueOf(InetAddress.getByName(ip)));
//            AsnResponse response = geoIp2Client.asn(InetAddress.getByName(ip));
            return "AS" + response.getAutonomousSystemNumber();
        } catch (Exception e) {
            AsnLookupServiceImpl.log.error("Failed to lookup ASN for IP: {}", ip, e);
            return null;
        }
    }

    /**
     * Performs an ASN lookup for the given IP address without using the cache.
     * This method first attempts to obtain the ASN via a DNS lookup.
     * If the DNS lookup fails, it falls back to a WHOIS lookup.
     *
     * @param ip the IP address to lookup
     * @return the ASN for the given IP address, or null if both lookups fail
     */
    @Override
    public String lookupAsnUncached(String ip) {
        String asn = lookupAsnViaDns(ip);
        return asn != null ? asn : lookupAsnViaWhois(ip);
    }

    /**
     * Performs an ASN lookup using DNS for the given IP address.
     * The method reverses the IP address, appends a specific ASN lookup domain,
     * and performs a DNS TXT record query to retrieve the Autonomous System Number (ASN).
     * The response is parsed to extract the ASN.
     * If the lookup fails, the method returns null.
     * This method is used as a fallback when the WHOIS lookup fails.
     * <p>
     * For more information on the ASN lookup domain, refer to the following link:
     * <a href="https://www.team-cymru.com/ip-asn-mapping">...</a>
     *
     * @param ip the IP address for which the ASN is to be looked up
     * @return the ASN as a string if found, or null if the lookup fails
     */
    @Override
    public String lookupAsnViaDns(String ip) {
        try {
            // Reverse the IP and append the ASN lookup domain
            String[] parts = ip.split("\\.");
            StringBuilder reversed = new StringBuilder();
            for (int i = parts.length - 1; i >= 0; i--) {
                reversed.append(parts[i]).append(".");
            }
            String lookupDomain = reversed + "origin.asn.cymru.com";

            Record[] records = new Lookup(lookupDomain, Type.TXT).run();
            if (records != null && records.length > 0) {
                String txt = ((org.xbill.DNS.TXTRecord) records[0]).getStrings().get(0);
                // Parse ASN from response (format: "ASN | IP | Country | Registry | Date")
                return txt.split("\\|")[0].trim();
            }
        } catch (Exception e) {
            AsnLookupServiceImpl.log.debug("DNS ASN lookup failed for IP: {}", ip, e);
        }
        return null;
    }

    /**
     * Performs an ASN lookup for the given IP address using WHOIS.
     * The method connects to the specified WHOIS server and sends a query
     * with the IP address as argument.
     * The response is then parsed to extract the Autonomous System Number (ASN).
     * If the lookup fails, the method returns null.
     *
     * @param ip the IP address for which the ASN is to be looked up
     * @return the ASN as a string if found, or null if the lookup fails
     */
    @Override
    public String lookupAsnViaWhois(String ip) {
        try (Socket socket = new Socket(whoisServer, 43);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send WHOIS query
            out.println(ip);

            // Read response
            String line;
            Pattern asnPattern = Pattern.compile("origin:\\s*AS(\\d+)", Pattern.CASE_INSENSITIVE);

            while ((line = in.readLine()) != null) {
                Matcher matcher = asnPattern.matcher(line);
                if (matcher.find()) {
                    return "AS" + matcher.group(1);
                }
            }
        } catch (Exception e) {
            AsnLookupServiceImpl.log.debug("WHOIS ASN lookup failed for IP: {}", ip, e);
        }
        return null;
    }

}
