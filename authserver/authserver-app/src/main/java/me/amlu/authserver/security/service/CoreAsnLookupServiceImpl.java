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

import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core implementation of AsnLookupService containing the actual lookup logic
 * (GeoIP2, DNS, WHOIS) without caching, rate limiting, or retries.
 * These concerns are handled by decorator services.
 */
@Service
@Qualifier("coreAsnLookup") // Identifies as the core bean for ASN lookup
public class CoreAsnLookupServiceImpl implements AsnLookupService {

    //    @Value("${web.whois.server}")
    @Value("${web.whois.server:whois.cymru.com}") // Provide a default WHOIS server
    private String whoisServer;

    private static final Logger log = LoggerFactory.getLogger(CoreAsnLookupServiceImpl.class);

    private final GeoIp2Service geoIp2Service;

    public CoreAsnLookupServiceImpl(GeoIp2Service geoIp2Service) {
        this.geoIp2Service = geoIp2Service;
    }

    /**
     * Looks up the Autonomous System Number (ASN) for a given IP address.
     * This core implementation tries GeoIP2 first, then falls back to DNS, then WHOIS.
     * It does NOT handle caching, rate limiting, or retries.
     *
     * @param ip the IP address to lookup
     * @return the ASN for the given IP address, or null if all lookup methods fail.
     */
    @Override
    public String lookupAsn(String ip) {
        log.trace("Performing core ASN lookup for IP: {}", ip);

        // 1. Try GeoIP2 lookup first (often the fastest if local DB)
        String asn = lookupAsnWithGeoIp2(ip);
        if (asn != null) {
            log.trace("ASN found via GeoIP2 for IP {}: {}", ip, asn);
            return asn;
        }
        log.trace("ASN not found via GeoIP2 for IP {}, falling back to DNS.", ip);

        // 2. Fallback to DNS lookup
        asn = lookupAsnViaDns(ip);
        if (asn != null) {
            log.trace("ASN found via DNS for IP {}: {}", ip, asn);
            return asn;
        }
        log.trace("ASN not found via DNS for IP {}, falling back to WHOIS.", ip);

        // 3. Fallback to WHOIS lookup
        asn = lookupAsnViaWhois(ip);
        if (asn != null) {
            log.trace("ASN found via WHOIS for IP {}: {}", ip, asn);
        } else {
            log.warn("ASN lookup failed for IP {} using all methods (GeoIP2, DNS, WHOIS).", ip);
        }
        return asn; // Return result from WHOIS (which might be null)
    }

    /**
     * Performs an ASN lookup for the given IP address using the MaxMind GeoIP2 database.
     * This core implementation does not handle rate limiting.
     *
     * @param ip the IP address to lookup
     * @return the ASN for the given IP address (e.g., "AS15169"), or null if the lookup fails.
     */
    @Override
    public String lookupAsnWithGeoIp2(String ip) {
        try {
            // Ensure geoIp2Service handles potential exceptions internally or declare them
            AsnResponse response = geoIp2Service.lookupAsn(ip); // Pass IP directly
            if (response != null && response.getAutonomousSystemNumber() != null) {
                return "AS" + response.getAutonomousSystemNumber();
            }
            log.trace("GeoIP2 lookup returned no ASN for IP: {}", ip);
            return null;
        } catch (GeoIp2Exception e) {
            // Catch specific exceptions from geoIp2Service if possible
            log.error("GeoIP2 ASN lookup failed for IP: {}", ip, e);
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
//        log.trace("lookupAsnUncached called, delegating to core lookupAsn for IP: {}", ip);
//        return lookupAsn(ip);
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
     * @return the ASN as a string (e.g., "AS15169") if found, or null if the lookup fails.
     */
    @CircuitBreaker(name = "dnsLookupCircuitBreaker", fallbackMethod = "lookupAsnViaWhois")
    @Override
    public String lookupAsnViaDns(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                log.debug("Invalid IP format for DNS ASN lookup: {}", ip);
                return null;
            }
            StringBuilder reversed = new StringBuilder();
            for (int i = parts.length - 1; i >= 0; i--) {
                reversed.append(parts[i]).append(".");
            }
            String lookupDomain = reversed + "origin.asn.cymru.com";

            Lookup lookup = new Lookup(lookupDomain, Type.TXT);
            Record[] records = lookup.run();

            if (lookup.getResult() == Lookup.SUCCESSFUL && records != null && records.length > 0) {
                // Ensure it's a TXT record before casting
                if (records[0] instanceof org.xbill.DNS.TXTRecord txtRecord) {
                    String txt = txtRecord.getStrings().get(0);
                    // Parse ASN from response (format: "ASN | IP | Country | Registry | Date")
                    String[] data = txt.split("\\|");
                    if (data.length > 0) {
                        String asnValue = data[0].trim();
                        // Basic validation if it looks like an ASN
                        if (asnValue.matches("(?i)AS\\d+")) {
                            return asnValue;
                        } else if (asnValue.matches("\\d+")) { // Handle if "AS" prefix is missing
                            return "AS" + asnValue;
                        } else {
                            log.warn("Unexpected format in DNS TXT record for {}: {}", ip, txt);
                        }
                    }
                } else {
                    log.warn("Expected TXT record but got {} for domain {}", records[0].getClass().getSimpleName(), lookupDomain);
                }
            } else {
                log.debug("DNS lookup unsuccessful for {}. Result: {}", lookupDomain, lookup.getErrorString());
            }
        } catch (TextParseException e) {
            log.error("Error parsing DNS lookup domain for IP {}: {}", ip, e.getMessage());
        } catch (Exception e) {
            // Catch broader exceptions but log specifically
            log.error("DNS ASN lookup failed unexpectedly for IP: {}", ip, e);
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
     * @return the ASN as a string (e.g., "AS15169") if found, or null if the lookup fails.
     */
//    IMPORTANT: At the moment, this is the last resort.
//    So if another last resort is given, change the fallback method in the CircuitBreaker annotation.
    @CircuitBreaker(name = "whoisLookupCircuitBreaker", fallbackMethod = "whoisFallback")
    @Override
    public String lookupAsnViaWhois(String ip) {
        // Use try-with-resources for Socket, PrintWriter, BufferedReader
        try (Socket socket = new Socket(whoisServer, 43);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            socket.setSoTimeout(5000); // Set a reasonable timeout (e.g., 5 seconds)

            // Send WHOIS query
            out.println(ip);

            // Read response
            String line;
            // Regex to find "origin:" or "OriginAS:" followed by AS number
            Pattern asnPattern = Pattern.compile("(?:origin|originas):\\s*AS(\\d+)", Pattern.CASE_INSENSITIVE);

            while ((line = in.readLine()) != null) {
                Matcher matcher = asnPattern.matcher(line);
                if (matcher.find()) {
                    return "AS" + matcher.group(1); // Return the found ASN
                }
            }
            log.debug("ASN not found in WHOIS response for IP: {}", ip);
        } catch (IOException e) {
            // More specific logging for IO errors
            log.error("WHOIS ASN lookup failed for IP {} due to IO error: {}", ip, e.getMessage());
        } catch (Exception e) {
            // Catch broader exceptions
            log.error("WHOIS ASN lookup failed unexpectedly for IP: {}", ip, e);
        }
        return null;
    }

    /**
     * Fallback method for WHOIS lookup in case of failure.
     * This method is called when the CircuitBreaker is triggered.
     *
     * @param ip the IP address for which the ASN is to be looked up
     * @param t  the exception that caused the fallback
     * @return null, indicating that the lookup failed
     */
    public String whoisFallback(String ip, Throwable t) {
        log.warn("WHOIS ASN lookup circuit breaker open or call failed for IP: {}. Returning null. Error: {}", ip, t.getMessage());
        return null; // Return null when WHOIS fails persistently
    }
}
