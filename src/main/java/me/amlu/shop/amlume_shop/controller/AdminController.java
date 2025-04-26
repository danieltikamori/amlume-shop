/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.controller;

import me.amlu.shop.amlume_shop.security.service.MfaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/")
public class AdminController {
   private final MfaService mfaService;

    public AdminController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

   @PostMapping("v1/admin/mfa/enforce")
   public ResponseEntity<String> enforceMfa(@RequestBody Map<String, Boolean> body) {
       Boolean enforced = body.get("enforced");
       if (enforced != null) {
           mfaService.updateMfaEnforced(enforced);
           return ResponseEntity.ok().build();
       } else {
           return ResponseEntity.badRequest().body("The 'enforced' parameter is required."); // Return a more descriptive error.
       }
   }
}