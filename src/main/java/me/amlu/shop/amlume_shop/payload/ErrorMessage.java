/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.payload;

import lombok.Getter;
import lombok.Setter;
import me.amlu.shop.amlume_shop.enums.ErrorCodeEnum;
import me.amlu.shop.amlume_shop.enums.ResponseMessage;
import org.springframework.http.HttpStatus;

import java.util.Date;

@Getter
public class ErrorMessage {
    @Setter
    private String message;

    @Setter
    private String title;

    private HttpStatus error;

    private int status;

    @Setter
    private String code;

    @Setter
    private String timestamp;

    @Setter
    private Object data;

    @Setter
    private String requestId;

    public ErrorMessage() {
    }

    public ErrorMessage(String message, HttpStatus error, ErrorCodeEnum code) {
        this.message = message;
        this.error = error;
        this.code = code.toString();
    }

    public ErrorMessage(String requestId, String message, HttpStatus error, ErrorCodeEnum code) {
        this(message, error, code);
        this.requestId = requestId;
    }

    public ErrorMessage(ResponseMessage message, HttpStatus error) {
        this(message.toString(), error, message.getErrorCode());
    }

    public ErrorMessage(String requestId, ResponseMessage message, HttpStatus error) {
        this(message.toString(), error, message.getErrorCode());
        this.requestId = requestId;
    }

    public ErrorMessage(ResponseMessage message, HttpStatus error, Object data) {
        this(message.toString(), error, message.getErrorCode());
        this.data = data;
    }

    public void setError(HttpStatus httpStatus) {
        this.error = httpStatus;
        this.status = httpStatus.value();
    }

    public int getStatus() {
        return error.value();
    }

    public void setStatus(int status) {
        error = HttpStatus.valueOf(status);
        this.status = status;
    }

    public String getTimestamp() {
        return new Date().toString();
    }

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
}