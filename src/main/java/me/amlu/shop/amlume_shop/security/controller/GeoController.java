/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.controller;

import me.amlu.shop.amlume_shop.security.enums.RiskLevel;
import me.amlu.shop.amlume_shop.security.service.AdvancedGeoService;
import me.amlu.shop.amlume_shop.security.service.GeoVerificationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geo")
public class GeoController {
    private final AdvancedGeoService geoService;

    public GeoController(AdvancedGeoService geoService) {
        this.geoService = geoService;
    }

    @PostMapping("/verify")
    public ResponseEntity<GeoVerificationResult> verifyLocation(
            @RequestHeader("X-Real-IP") String ip,
            @RequestHeader("X-User-ID") String userId) {

        GeoVerificationResult result = geoService.verifyLocation(ip, userId);

        if (result.getRisk() == RiskLevel.HIGH) {
            // Trigger additional authentication
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(result);
        }

        return ResponseEntity.ok(result);
    }
}
