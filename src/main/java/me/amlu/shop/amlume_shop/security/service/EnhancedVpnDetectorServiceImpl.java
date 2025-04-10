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

import java.net.UnknownHostException;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Slf4j
@Service
public class EnhancedVpnDetectorServiceImpl implements EnhancedVpnDetectorService {

    @Value(("${vpn.min-suspicious-factors}"))
    private int minSuspiciousFactors;
    private final Set<String> knownVpnAsns;
    private final Set<String> knownVpnIpRanges;
    private final Set<String> knownDatacenterRanges;

    private final AsnLookupService asnLookupService;
    private final AsnReputationService reputationService;
    private final EnhancedVpnDetectorService vpnDetector;

    public EnhancedVpnDetectorServiceImpl(Set<String> knownVpnAsns, Set<String> knownVpnIpRanges, Set<String> knownDatacenterRanges, AsnLookupService asnLookupService, AsnReputationService reputationService, EnhancedVpnDetectorService vpnDetector) {
        this.knownVpnAsns = knownVpnAsns;
        this.knownVpnIpRanges = knownVpnIpRanges;
        this.knownDatacenterRanges = knownDatacenterRanges;
        this.asnLookupService = asnLookupService;
        this.reputationService = reputationService;
        this.vpnDetector = vpnDetector;
    }

    public boolean isVpnConnection(String ip) {
        String asn = asnLookupService.lookupAsn(ip);
        if (asn == null) {
            log.warn("Could not determine ASN for IP: {}", ip);
            return false;
        }

        boolean isVpn = vpnDetector.isLikelyVpn(ip, asn);
        reputationService.recordActivity(asn, isVpn);

        double reputation = reputationService.getReputationScore(asn);
        return isVpn || reputation < 0.3;
    }

    @Override
    public boolean isLikelyVpn(String ip, String asn) {
        return checkMultipleFactors(ip, asn);
    }

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
