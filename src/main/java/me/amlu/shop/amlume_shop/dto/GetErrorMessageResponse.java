/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.dto;

import me.amlu.shop.amlume_shop.enums.ErrorCodeEnum;
import me.amlu.shop.amlume_shop.enums.ResponseMessage;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

import java.util.Date;

public record GetErrorMessageResponse(String message, String title, HttpStatus error, int status, String code,
                                      String timestamp, Object data, String requestId) {

    public GetErrorMessageResponse(String message, HttpStatus error, ErrorCodeEnum code) {
        this(message, null, error, error.value(), code.toString(), new Date().toString(), null, null);
    }

    public GetErrorMessageResponse(String requestId, String message, HttpStatus error, ErrorCodeEnum code) {
        this(message, null, error, error.value(), code.toString(), new Date().toString(), null, requestId);
    }

    public GetErrorMessageResponse(ResponseMessage message, HttpStatus error) {
        this(message.toString(), error, message.getErrorCode());
    }

    public GetErrorMessageResponse(String requestId, ResponseMessage message, HttpStatus error) {
        this(requestId, message.toString(), error, message.getErrorCode());
    }

    public GetErrorMessageResponse(ResponseMessage message, HttpStatus error, Object data) {
        this(message.toString(), error, message.getErrorCode(), data);
    }

    @NotNull
    @Override
    public String toString() {
        return "ErrorMessage{" +
                "message='" + message + '\'' +
                ", title='" + title + '\'' +
                ", error=" + error +
                ", status=" + status +
                ", code='" + code + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", data=" + data +
                ", requestId='" + requestId + '\'' +
                '}';
    }

    public GetErrorMessageResponse(String message, HttpStatus error, ErrorCodeEnum errorCode, Object data) {
        this(message, null, error, error.value(), errorCode.toString(), new Date().toString(), data, null);
    }


}