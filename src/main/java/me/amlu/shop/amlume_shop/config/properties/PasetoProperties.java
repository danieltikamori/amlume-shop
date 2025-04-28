/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for PASETO token generation and validation.
 * Maps properties under the 'paseto' prefix in application configuration files (e.g., application.yml).
 * <p>
 * Example YAML structure:
 * <pre>{@code
 * paseto:
 *   token-no-footer:
 *     parts: 3
 *   token-with-footer:
 *     parts: 4
 *   access:
 *     expiration: 3600s # Default access token expiration
 *     local:
 *       secret-key: ${PASETO_ACCESS_SECRET_KEY:}
 *       kid: ${PASETO_ACCESS_LOCAL_KID:}
 *       # expiration: 3600s # Optional override
 *     public:
 *       private-key: ${PASETO_ACCESS_PRIVATE_KEY:}
 *       public-key: ${PASETO_ACCESS_PUBLIC_KEY:}
 *       kid: ${PASETO_ACCESS_PUBLIC_KID:}
 *       # expiration: 3600s # Optional override
 *   refresh:
 *     expiration: 86400s # Default refresh token expiration
 *     local:
 *       secret-key: ${PASETO_REFRESH_SECRET_KEY:}
 *       kid: ${PASETO_REFRESH_LOCAL_KID:}
 *       # expiration: 86400s # Optional override
 *     public:
 *       private-key: ${PASETO_REFRESH_PRIVATE_KEY:}
 *       public-key: ${PASETO_REFRESH_PUBLIC_KEY:}
 *       kid: ${PASETO_REFRESH_PUBLIC_KID:}
 *       # expiration: 86400s # Optional override
 * }</pre>
 */

@Configuration // Make it a configuration bean
@ConfigurationProperties(prefix = "paseto")
public class PasetoProperties {

    private int tokenNoFooterParts;
    private int tokenWithFooterParts;
    private Access access;
    private Refresh refresh;

    // --- GETTERS ---
    // ... (getters for all fields)

    // --- SETTERS ---
    public void setTokenNoFooterParts(int tokenNoFooterParts) {
        this.tokenNoFooterParts = tokenNoFooterParts;
    }

    public void setTokenWithFooterParts(int tokenWithFooterParts) {
        this.tokenWithFooterParts = tokenWithFooterParts;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public void setRefresh(Refresh refresh) {
        this.refresh = refresh;
    }

    // --- Nested Classes ---
    public static class Access {
        private long expiration;
        private Local local;
        private Public pub; // Renamed to avoid conflict with keyword 'public'

        // --- GETTERS ---
        // ...

        // --- SETTERS ---
        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }

        public void setLocal(Local local) {
            this.local = local;
        }

        public void setPub(Public pub) {
            this.pub = pub;
        } // Setter for 'pub'

        // Nested Local class
        public static class Local {
            private String secretKey;
            private String kid;

            // --- GETTERS ---
            // ...
            // --- SETTERS ---
            public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
            }

            public void setKid(String kid) {
                this.kid = kid;
            }
        }

        // Nested Public class
        public static class Public { // Renamed field in outer class to 'pub'
            private String privateKey; // Field name matches 'private-key'
            private String publicKey;
            private String kid;
            private long expiration; // Optional override

            // --- GETTERS ---
            // ...

            // --- SETTERS ---
            public void setPrivateKey(String privateKey) { // Setter for 'privateKey'
                this.privateKey = privateKey;
            }

            public void setPublicKey(String publicKey) {
                this.publicKey = publicKey;
            }

            public void setKid(String kid) {
                this.kid = kid;
            }

            public void setExpiration(long expiration) {
                this.expiration = expiration;
            }
        }
    }

    public static class Refresh {
        // Similar structure with getters and setters for refresh properties
        private long expiration;
        private Local local;
        private Public pub; // Renamed

        // ... getters and setters ...

        public static class Local {
            private String secretKey;
            private String kid;
            // ... getters and setters ...
        }

        public static class Public { // Renamed field in outer class to 'pub'
            private String privateKey;
            private String publicKey;
            private String kid;
            private long expiration;
            // ... getters and setters ...
        }
    }

    // --- Helper Getters used by KeyManagementFacade ---
    // These extract values from the nested structure

    public String getAccessPublicKey() {
        return (access != null && access.pub != null) ? access.pub.publicKey : null;
    }

    public String getAccessPrivateKey() {
        return (access != null && access.pub != null) ? access.pub.privateKey : null;
    }

    public String getAccessSecretKey() {
        return (access != null && access.local != null) ? access.local.secretKey : null;
    }

    public String getAccessPublicKid() {
        return (access != null && access.pub != null) ? access.pub.kid : null;
    }

    public String getAccessLocalKid() {
        return (access != null && access.local != null) ? access.local.kid : null;
    }

    public String getRefreshPublicKey() {
        return (refresh != null && refresh.pub != null) ? refresh.pub.publicKey : null;
    }

    public String getRefreshPrivateKey() {
        return (refresh != null && refresh.pub != null) ? refresh.pub.privateKey : null;
    }

    public String getRefreshSecretKey() {
        return (refresh != null && refresh.local != null) ? refresh.local.secretKey : null;
    }

    public String getRefreshPublicKid() {
        return (refresh != null && refresh.pub != null) ? refresh.pub.kid : null;
    }

    public String getRefreshLocalKid() {
        return (refresh != null && refresh.local != null) ? refresh.local.kid : null;
    }
}
