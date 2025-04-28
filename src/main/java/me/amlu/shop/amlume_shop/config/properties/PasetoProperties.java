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
import org.springframework.stereotype.Component;

import java.time.Duration;

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
@ConfigurationProperties(prefix = "paseto")
@Component
//Setters - REQUIRED for @ConfigurationProperties binding
public class PasetoProperties {

    private TokenConfig tokenNoFooter = new TokenConfig();
    private TokenConfig tokenWithFooter = new TokenConfig();
    private AccessProperties access = new AccessProperties();
    private RefreshProperties refresh = new RefreshProperties();

    public TokenConfig getTokenNoFooter() {
        return this.tokenNoFooter;
    }

    public TokenConfig getTokenWithFooter() {
        return this.tokenWithFooter;
    }

    public AccessProperties getAccess() {
        return this.access;
    }

    public RefreshProperties getRefresh() {
        return this.refresh;
    }

    public void setTokenNoFooter(TokenConfig tokenNoFooter) {
        this.tokenNoFooter = tokenNoFooter;
    }

    public void setTokenWithFooter(TokenConfig tokenWithFooter) {
        this.tokenWithFooter = tokenWithFooter;
    }

    public void setAccess(AccessProperties access) {
        this.access = access;
    }

    public void setRefresh(RefreshProperties refresh) {
        this.refresh = refresh;
    }

    public static class TokenConfig {
        private int parts;

        public int getParts() {
            return this.parts;
        }

        public void setParts(int parts) {
            this.parts = parts;
        }
    }

    public static class AccessProperties {
        private Duration expiration; // Default expiration for all access tokens
        private LocalProperties local = new LocalProperties();
        private PublicProperties publicProps = new PublicProperties(); // Renamed to avoid conflict with keyword

        public Duration getExpiration() {
            return this.expiration;
        }

        public LocalProperties getLocal() {
            return this.local;
        }

        public PublicProperties getPublicProps() {
            return this.publicProps;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }

        public void setLocal(LocalProperties local) {
            this.local = local;
        }

        public void setPublicProps(PublicProperties publicProps) {
            this.publicProps = publicProps;
        }

        public static class LocalProperties {
            private String secretKey;
            private String kid;
            private Duration expiration; // Optional override for local access tokens

            public String getSecretKey() {
                return this.secretKey;
            }

            public String getKid() {
                return this.kid;
            }

            public Duration getExpiration() {
                return this.expiration;
            }

            public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
            }

            public void setKid(String kid) {
                this.kid = kid;
            }

            public void setExpiration(Duration expiration) {
                this.expiration = expiration;
            }
        }

        public static class PublicProperties {
            private String privateKey;
            private String publicKey;
            private String kid;
            private Duration expiration; // Optional override for public access tokens

            public String getPrivateKey() {
                return this.privateKey;
            }

            public String getPublicKey() {
                return this.publicKey;
            }

            public String getKid() {
                return this.kid;
            }

            public Duration getExpiration() {
                return this.expiration;
            }

            public void setPrivateKey(String privateKey) {
                this.privateKey = privateKey;
            }

            public void setPublicKey(String publicKey) {
                this.publicKey = publicKey;
            }

            public void setKid(String kid) {
                this.kid = kid;
            }

            public void setExpiration(Duration expiration) {
                this.expiration = expiration;
            }
        }
    }

    public static class RefreshProperties {
        private Duration expiration; // Default expiration for all refresh tokens
        private LocalProperties local = new LocalProperties();
        private PublicProperties publicProps = new PublicProperties(); // Renamed to avoid conflict with keyword

        public Duration getExpiration() {
            return this.expiration;
        }

        public LocalProperties getLocal() {
            return this.local;
        }

        public PublicProperties getPublicProps() {
            return this.publicProps;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }

        public void setLocal(LocalProperties local) {
            this.local = local;
        }

        public void setPublicProps(PublicProperties publicProps) {
            this.publicProps = publicProps;
        }

        public static class LocalProperties {
            private String secretKey;
            private String kid;
            private Duration expiration; // Optional override for local refresh tokens

            public String getSecretKey() {
                return this.secretKey;
            }

            public String getKid() {
                return this.kid;
            }

            public Duration getExpiration() {
                return this.expiration;
            }

            public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
            }

            public void setKid(String kid) {
                this.kid = kid;
            }

            public void setExpiration(Duration expiration) {
                this.expiration = expiration;
            }
        }

        public static class PublicProperties {
            private String privateKey;
            private String publicKey;
            private String kid;
            private Duration expiration; // Optional override for public refresh tokens

            public String getPrivateKey() {
                return this.privateKey;
            }

            public String getPublicKey() {
                return this.publicKey;
            }

            public String getKid() {
                return this.kid;
            }

            public Duration getExpiration() {
                return this.expiration;
            }

            public void setPrivateKey(String privateKey) {
                this.privateKey = privateKey;
            }

            public void setPublicKey(String publicKey) {
                this.publicKey = publicKey;
            }

            public void setKid(String kid) {
                this.kid = kid;
            }

            public void setExpiration(Duration expiration) {
                this.expiration = expiration;
            }
        }
    }

    // --- Convenience Getters to access nested properties easily ---
    // These are optional but can make usage cleaner in services

    public Duration getAccessLocalExpiration() {
        // Return specific local expiration if set, otherwise default access expiration
        return access.getLocal().getExpiration() != null ? access.getLocal().getExpiration() : access.getExpiration();
    }

    public Duration getAccessPublicExpiration() {
        // Return specific public expiration if set, otherwise default access expiration
        return access.getPublicProps().getExpiration() != null ? access.getPublicProps().getExpiration() : access.getExpiration();
    }

    public Duration getRefreshLocalExpiration() {
        // Return specific local expiration if set, otherwise default refresh expiration
        return refresh.getLocal().getExpiration() != null ? refresh.getLocal().getExpiration() : refresh.getExpiration();
    }

    public Duration getRefreshPublicExpiration() {
        // Return specific public expiration if set, otherwise default refresh expiration
        return refresh.getPublicProps().getExpiration() != null ? refresh.getPublicProps().getExpiration() : refresh.getExpiration();
    }

    public String getAccessPublicKey() {
        return access.getPublicProps().getPublicKey();
    }

    public String getRefreshPublicKey() {
        return refresh.getPublicProps().getPublicKey();
    }

    public String getAccessPrivateKey() {
        return access.getPublicProps().getPrivateKey();
    }

    public String getRefreshPrivateKey() {
        return refresh.getPublicProps().getPrivateKey();
    }

    public String getAccessSecretKey() {
        return access.getLocal().getSecretKey();
    }

    public String getRefreshSecretKey() {
        return refresh.getLocal().getSecretKey();
    }

    public String getAccessPublicKid() {
        return access.getPublicProps().getKid();
    }

    public String getAccessLocalKid() {
        return access.getLocal().getKid();
    }

    public String getRefreshLocalKid() {
        return refresh.getLocal().getKid();
    }

    public String getRefreshPublicKid() {
        return refresh.getPublicProps().getKid();
    }

    public int getTokenNoFooterParts() {
        return tokenNoFooter.getParts();
    }

    public int getTokenWithFooterParts() {
        return tokenWithFooter.getParts();
    }
}
