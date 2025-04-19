/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.payload.user;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public record AuthResponse(
        String token,
        String username,
        Collection<? extends GrantedAuthority> authorities,
        String refreshToken,
        String accessToken,
        boolean mfaEnabled,
        boolean mfaRequired,
        String message,
        boolean success,
        Map<String, Object> details,
        String secretImageUrl
) {

    public AuthResponse(String message, String token, boolean success) {
        this(token, null, null, null, null, false, false, message, success, null, null);
    }

    public AuthResponse(String token, String name, Collection<? extends GrantedAuthority> authorities, boolean success, String loginSuccessful) {
        this(token, name, authorities, null, null, false, false, loginSuccessful, success, null, null);
    }

    public AuthResponse(String token, String refreshToken, String accessToken) {
        this(token, null, null, refreshToken, accessToken, false, false, null, true, null, null);
    }

    public AuthResponse() {
        this(null, null, null, null, null, false, false, null, false, null, null);
    }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public boolean isMfaRequired() {
        return details != null && details.containsKey("mfaRequired") && (boolean) details.get("mfaRequired");
    }

    public static class AuthResponseBuilder {
        private String token;
        private String username;
        private Collection<? extends GrantedAuthority> authorities;
        private String refreshToken;
        private String accessToken;
        private boolean mfaEnabled;
        private boolean mfaRequired;
        private String message;
        private boolean success;
        private Map<String, Object> details;
        private String secretImageUrl;

        AuthResponseBuilder() {
        }

        public AuthResponseBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public AuthResponseBuilder authorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public AuthResponseBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AuthResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public AuthResponseBuilder mfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
            return this;
        }

        public AuthResponseBuilder mfaRequired(boolean mfaRequired) {
            this.mfaRequired = mfaRequired;
            return this;
        }

        public AuthResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public AuthResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public AuthResponseBuilder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public AuthResponseBuilder secretImageUrl(String secretImageUrl) {
            this.secretImageUrl = secretImageUrl;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, username, authorities, refreshToken, accessToken, mfaEnabled, mfaRequired, message, success, details, secretImageUrl);
        }

        @Override
        public String toString() {
            return "AuthResponse.AuthResponseBuilder(token=" + this.token + ", username=" + this.username + ", authorities=" + this.authorities + ", refreshToken=" + this.refreshToken + ", accessToken=" + this.accessToken + ", mfaEnabled=" + this.mfaEnabled + ", mfaRequired=" + this.mfaRequired + ", message=" + this.message + ", success=" + this.success + ", details=" + this.details + ", secretImageUrl=" + this.secretImageUrl + ")";
        }
    }


//    public static class AuthResponseBuilder {
//        private String token;
//        private String refreshToken;
//        private String accessToken;
//        private boolean mfaEnabled;
//
//        public AuthResponseBuilder token(String token) {
//            this.token = token;
//            return this;
//        }
//
//        public AuthResponseBuilder refreshToken(String refreshToken) {
//            this.refreshToken = refreshToken;
//            return this;
//        }
//
//        public AuthResponseBuilder accessToken(String accessToken) {
//            this.accessToken = accessToken;
//            return this;
//        }
//
//        public AuthResponseBuilder mfaEnabled(boolean mfaEnabled) {
//            this.mfaEnabled = mfaEnabled;
//            return this;
//        }
//
//        // other methods...
//
//        public AuthResponse build() {
//            return new AuthResponse(
//                    token,
//                    username,
//                    authorities,
//                    refreshToken,
//                    accessToken,
//                    mfaEnabled,
//                    mfaRequired,
//                    message,
//                    success,
//                    details,
//                    secretImageUrl
//            );
//        }
//
//        public AuthResponseBuilder secretImageUrl(String s) {
//            if (details == null) {
//                details = new java.util.HashMap<>();
//            }
//            details.put("secretImageUrl", s);
//            return this;
//        }

//        public AuthResponseBuilder mfaEnabled(boolean mfaEnabled) {
//            if (details == null) {
//                details = new java.util.HashMap<>();
//            }
//            details.put("mfaEnabled", mfaEnabled);
//            return this;
//        }
//    }

//        @Builder(builderClassName = "AuthResponseBuilder", toBuilder = true)
//        public AuthResponse(String token, String username, Collection<? extends GrantedAuthority> authorities, String refreshToken, String accessToken, boolean mfaEnabled, boolean mfaRequired, String message, boolean success, Map<String, Object> details, String secretImageUrl) {
//            this.token = token;
//            this.username = username;
//            this.authorities = authorities;
//            this.refreshToken = refreshToken;
//            this.accessToken = accessToken;
//            this.mfaEnabled = mfaEnabled;
//            this.mfaRequired = mfaRequired;
//            this.message = message;
//            this.success = success;
//            this.details = details;
//            this.secretImageUrl = secretImageUrl;
//        }
}

