/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;

@Component
public class AsnConfigLoader {
    private Set<String> knownVpnAsns;
    
    @Value("${asn.config.path}")
    private String configPath;
    
    @PostConstruct
    public void loadAsnConfig() {
        knownVpnAsns = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && !line.isEmpty()) {
                    knownVpnAsns.add(line.trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load ASN configuration", e);
        }
    }
    
    public Set<String> getKnownVpnAsns() {
        return Collections.unmodifiableSet(knownVpnAsns);
    }
}
