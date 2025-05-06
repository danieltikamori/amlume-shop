/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.notification.dto;

import org.springframework.http.HttpStatus;
import me.amlu.shop.amlume_shop.enums.ResponseMessage;

public record GetEmailResponse(String message, Object data, int status) {
    public GetEmailResponse(String message) {
        this(message, null, HttpStatus.OK.value());
    }
    public GetEmailResponse(String message, Object data) {
        this(message, data, HttpStatus.OK.value());
    }
    public GetEmailResponse(ResponseMessage responseMessage) {
        this(responseMessage.getMessage(), null, HttpStatus.OK.value());
    }
    public GetEmailResponse(String message, String data, ResponseMessage responseMessage) {
        this(responseMessage.getMessage(), data, HttpStatus.OK.value());
    }
}