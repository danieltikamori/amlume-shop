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

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

@Service
public class EnhancedVpnDetectorServiceImpl implements EnhancedVpnDetectorService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EnhancedVpnDetectorServiceImpl.class);

    @Value(("${security.vpn.min-suspicious-factors:2}"))
    private int minSuspiciousFactors;

    //    @Value(("${security.geo.known-vpn-asns}"))
    private final Set<String> knownVpnAsns;

    //    @Value(("${security.geo.known-vpn-ip-ranges}"))
    private final Set<String> knownVpnIpRanges;

    //    @Value(("${security.geo.known-datacenter-ranges}"))
    private final Set<String> knownDatacenterRanges;

    private final AsnLookupService asnLookupService;
    private final AsnReputationService reputationService;

    public EnhancedVpnDetectorServiceImpl(
            Set<String> knownVpnAsns,
            Set<String> knownVpnIpRanges,
            Set<String> knownDatacenterRanges,
            @Qualifier("retryingAsnLookup") AsnLookupService asnLookupService,
            AsnReputationService reputationService
    ) {
        this.knownVpnAsns = knownVpnAsns;
        this.knownVpnIpRanges = knownVpnIpRanges;
        this.knownDatacenterRanges = knownDatacenterRanges;
        this.asnLookupService = asnLookupService;
        this.reputationService = reputationService;
    }

    public boolean isVpnConnection(String ip) {
        String asn = asnLookupService.lookupAsn(ip);
        if (asn == null) {
            log.warn("Could not determine ASN for IP: {}", ip);
            return false; // Or handle as potentially suspicious based on policy
        }

        // Call OWN implementation of isLikelyVpn
        boolean isVpn = this.isLikelyVpn(ip, asn); // CHANGED: Use 'this' instead of 'vpnDetector'

        reputationService.recordActivity(asn, isVpn);

        double reputation = reputationService.getReputationScore(asn);
        // Consider if reputation check should happen even if ASN lookup failed
        return isVpn || reputation < 0.3; // Adjust threshold as needed
    }

    @Override
    public boolean isLikelyVpn(String ip, String asn) {
        // This method remains the same, implementing the core logic
        return checkMultipleFactors(ip, asn);
    }

    // ... rest of the methods (checkMultipleFactors, isInVpnRange, etc.) remain the same ...
    @Override
    public boolean checkMultipleFactors(String ip, String asn) {
        int suspiciousFactors = 0;

        // Check if IP is in known VPN ranges
        if (isInVpnRange(ip)) suspiciousFactors++;

        // Check if ASN is known VPN provider
        if (knownVpnAsns.contains(asn)) suspiciousFactors++;

        // Check if IP is in datacenter range
        if (isInDatacenterRange(ip)) suspiciousFactors++;

        // Check for reverse DNS patterns
        if (hasVpnRelatedDns(ip)) suspiciousFactors++;

        // Check connection patterns (if available)
        if (hasAbnormalConnectionPatterns(ip)) suspiciousFactors++;

        return suspiciousFactors >= minSuspiciousFactors; // Threshold can be adjusted
    }

    private boolean isInVpnRange(String ip) {
        // Implement the logic to check if the IP address is in a known VPN range
        // For example, you can use a database or a file to store the known VPN ranges
        // and then check if the IP address is in any of those ranges
        return knownVpnIpRanges.contains(ip);
    }

    private boolean isInDatacenterRange(String ip) {
        // Implement the logic to check if the IP address is in a datacenter range
        return knownDatacenterRanges.contains(ip);
    }

    @Override
    public boolean hasVpnRelatedDns(String ip) {
        try {
            String reverseDns = InetAddress.getByName(ip).getCanonicalHostName();
            return reverseDns.toLowerCase().matches(".*(vpn|proxy|tor|exit|node).*");
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean hasAbnormalConnectionPatterns(String ip) {
        // Implement the logic to check if the IP address has abnormal connection patterns
        // For example, you can use a database or a file to store the known abnormal connection patterns
        // and then check if the IP address matches any of those patterns
        // This method is currently not implemented, so it will always return false
        return false;
    }
}
