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

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.payload.CreateEmailRequest;
import me.amlu.shop.amlume_shop.payload.GetResponse;
import me.amlu.shop.amlume_shop.security.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/emailService")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin("*")
public class EmailController {
    private final EmailService emailService;

    @PostMapping("/send")
    @Operation(summary = "Send EMail", description = "Send Email to the given Email Address")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = GetResponse.class)))
    public ResponseEntity<Object> sendMail(@Valid @RequestBody CreateEmailRequest createEmailRequest) {
        log.info("HIT /send POST | Dto : {}", createEmailRequest);
        return ResponseEntity.ok(emailService.sendMail(createEmailRequest));
    }

}