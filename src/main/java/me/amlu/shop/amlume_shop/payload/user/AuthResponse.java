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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AuthResponse {

    private String token;
    private String username;
    private Collection<? extends GrantedAuthority> authorities;
    private String refreshToken;
    private String accessToken;

    private boolean mfaEnabled;
    private boolean mfaRequired;
    private String message;
    private boolean success;
    private Map<String, Object> details;  // Map holding any MFA details
    private String secretImageUrl;


    public AuthResponse(String message, String token, boolean success) {
        this.message = message;
        this.token = token;
        this.success = true;
    }

    public AuthResponse(String token, String name, Collection<? extends GrantedAuthority> authorities, boolean success, String loginSuccessful) {
        this.token = token;
        this.username = name;
        this.authorities = authorities;
        this.success = success;
        this.message = loginSuccessful;
    }

    public AuthResponse(String token, String refreshToken, String accessToken) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.success = true;
    }

    public boolean isMfaRequired() {
        return details != null && details.containsKey("mfaRequired") && (boolean) details.get("mfaRequired");
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

