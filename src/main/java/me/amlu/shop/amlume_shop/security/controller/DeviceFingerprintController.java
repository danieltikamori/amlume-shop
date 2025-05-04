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

import me.amlu.shop.amlume_shop.security.service.DeviceFingerprintService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-fingerprint")
public class DeviceFingerprintController {

    private final DeviceFingerprintService deviceFingerprintService;
    private final UserService userService;

    public DeviceFingerprintController(DeviceFingerprintService deviceFingerprintService, UserService userService) {
        this.deviceFingerprintService = deviceFingerprintService;
        this.userService = userService;
    }

    @PostMapping("/v1/disable")
    public ResponseEntity<Void> disableDeviceFingerprinting(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        deviceFingerprintService.disableDeviceFingerprinting(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/enable")
    public ResponseEntity<Void> enableDeviceFingerprinting(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        deviceFingerprintService.enableDeviceFingerprinting(user);
        return ResponseEntity.ok().build();
    }
}