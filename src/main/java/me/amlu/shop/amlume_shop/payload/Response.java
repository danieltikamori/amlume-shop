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
import me.amlu.shop.amlume_shop.enums.ResponseMessage;
import org.springframework.http.HttpStatus;

@Setter
@Getter
public class Response {

    private String message;
    private Object data;
    private int status = HttpStatus.OK.value();

    public Response(String message) {
        this.message = message;
    }

    private Response(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public Response(ResponseMessage responseMessage) {
        this.message = responseMessage.getMessage();
    }

    public Response(String message, String data, ResponseMessage responseMessage) {
        this.message = message;
        this.data = data;
        this.message = responseMessage.getMessage();
    }

}