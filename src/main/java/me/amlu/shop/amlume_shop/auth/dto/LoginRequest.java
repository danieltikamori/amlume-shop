/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        String mfaCode,
        @NotBlank String captchaResponse,
        String userAgent,
        String screenWidth,
        String screenHeight,
        boolean rememberMe
) {

    public LoginRequest() {
        this(null, null, null, null, null, null, null, false);
    }

    public static LoginRequestBuilder builder() {
        return new LoginRequestBuilder();
    }

    public static class LoginRequestBuilder {
        private String username;
        private String password;
        private String mfaCode;
        private String captchaResponse;
        private String userAgent;
        private String screenWidth;
        private String screenHeight;
        private boolean rememberMe = false; // Default value for boolean

        public LoginRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public LoginRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public LoginRequestBuilder mfaCode(String mfaCode) {
            this.mfaCode = mfaCode;
            return this;
        }

        public LoginRequestBuilder captchaResponse(String captchaResponse) {
            this.captchaResponse = captchaResponse;
            return this;
        }

        public LoginRequestBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public LoginRequestBuilder screenWidth(String screenWidth) {
            this.screenWidth = screenWidth;
            return this;
        }

        public LoginRequestBuilder screenHeight(String screenHeight) {
            this.screenHeight = screenHeight;
            return this;
        }

        public LoginRequestBuilder rememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
            return this;
        }

        public LoginRequest build() {
            return new LoginRequest(username, password, mfaCode, captchaResponse, userAgent, screenWidth, screenHeight, rememberMe);
        }
    }
}

