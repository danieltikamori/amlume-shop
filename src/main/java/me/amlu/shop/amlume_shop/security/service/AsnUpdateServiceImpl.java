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

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.config.AsnConfigLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@EnableScheduling
@Service
public class AsnUpdateServiceImpl implements AsnUpdateService {
    private final AsnConfigLoader asnConfigLoader;

    public AsnUpdateServiceImpl(AsnConfigLoader asnConfigLoader) {
        this.asnConfigLoader = asnConfigLoader;
    }

    @Scheduled(cron = "${asn.update.schedule}")
    public void updateAsnList() {
        // Fetch latest ASN data from a reliable source
        // Example: MaxMind, RIPE NCC, or your preferred provider
        try {
            // Download updated ASN list
            // Parse and validate the new data
            // Update the configuration file
            asnConfigLoader.loadAsnConfig();
        } catch (Exception e) {
            log.error("Failed to update ASN list", e);
        }
    }
}
