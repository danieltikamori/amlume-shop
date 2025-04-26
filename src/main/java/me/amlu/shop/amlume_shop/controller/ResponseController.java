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

import me.amlu.shop.amlume_shop.payload.GetErrorMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseController {
    protected ResponseEntity<Object> sendResponse(Object object) {
        if (object instanceof GetErrorMessageResponse err) {
            return sendResponse(object, err.error());
        }
        return sendResponse(object, HttpStatus.OK);
    }

    private ResponseEntity<Object> sendResponse(Object object, HttpStatus httpStatus) {
        return ResponseEntity.status(httpStatus)
                .header("Content-Type", "application/json")
                .body(object);
    }
}