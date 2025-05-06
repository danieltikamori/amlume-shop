/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration properties for PASETO token generation and validation.
 * Maps properties under the 'paseto' prefix.
 * NEW STRUCTURE: paseto.public.* and paseto.local.*
 * <p>
 * Example YAML structure:
 * <pre>{@code
 * paseto:
 *   token-no-footer-parts: 3
 *   token-with-footer-parts: 4
 *   public: # Public (Asymmetric) Keys
 *     access:
 *       # private-key: from vault (PEM format)
 *       # public-key:  from vault (PEM format)
 *       # kid:         from vault
 *       expiration: 3600s # Optional override
 *     refresh: # If using asymmetric refresh keys
 *       # private-key: from vault (PEM format)
 *       # public-key:  from vault (PEM format)
 *       # kid:         from vault
 *       # expiration: 86400s # Optional override
 *   local: # Local (Symmetric) Keys
 *     access:
 *       # secret-key: from vault
 *       # kid:        from vault
 *       # expiration: 3600s # Optional override
 *     refresh:
 *       # secret-key: from vault
 *       # kid:        from vault
 *       # expiration: 86400s # Optional override
 * }</pre>
 */
@Configuration
@ConfigurationProperties(prefix = "paseto")
public class PasetoProperties {

    private int tokenNoFooterParts;
    private int tokenWithFooterParts;
    private Public pub = new Public(); // Renamed from 'access'
    private Local local = new Local(); // Renamed from 'refresh'

    // --- Standard Getters/Setters ---
    public int getTokenNoFooterParts() {
        return this.tokenNoFooterParts;
    }

    public void setTokenNoFooterParts(int tokenNoFooterParts) {
        this.tokenNoFooterParts = tokenNoFooterParts;
    }

    public int getTokenWithFooterParts() {
        return this.tokenWithFooterParts;
    }

    public void setTokenWithFooterParts(int tokenWithFooterParts) {
        this.tokenWithFooterParts = tokenWithFooterParts;
    }

    public Public getPub() {
        return this.pub;
    } // Getter for 'public' properties

    public void setPub(Public pub) {
        this.pub = pub;
    }

    public Local getLocal() {
        return this.local;
    } // Getter for 'local' properties

    public void setLocal(Local local) {
        this.local = local;
    }

    // --- equals, hashCode, toString ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasetoProperties that = (PasetoProperties) o;
        return tokenNoFooterParts == that.tokenNoFooterParts &&
                tokenWithFooterParts == that.tokenWithFooterParts &&
                Objects.equals(pub, that.pub) &&
                Objects.equals(local, that.local);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenNoFooterParts, tokenWithFooterParts, pub, local);
    }

    @Override
    public String toString() {
        return "PasetoProperties{" +
                "tokenNoFooterParts=" + tokenNoFooterParts +
                ", tokenWithFooterParts=" + tokenWithFooterParts +
                ", pub=" + pub + // Use updated field name
                ", local=" + local + // Use updated field name
                '}';
    }

    // --- Nested class for Public (Asymmetric) Keys ---
    public static class Public {
        private KeyConfig access = new KeyConfig();
        private KeyConfig refresh = new KeyConfig(); // Add refresh if needed

        public Public() {
        }

        public KeyConfig getAccess() {
            return this.access;
        }

        public void setAccess(KeyConfig access) {
            this.access = access;
        }

        public KeyConfig getRefresh() {
            return this.refresh;
        }

        public void setRefresh(KeyConfig refresh) {
            this.refresh = refresh;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Public aPublic = (Public) o;
            return Objects.equals(access, aPublic.access) && Objects.equals(refresh, aPublic.refresh);
        }

        @Override
        public int hashCode() {
            return Objects.hash(access, refresh);
        }

        @Override
        public String toString() {
            return "Public{" +
                    "access=" + access +
                    ", refresh=" + refresh +
                    '}';
        }

        // Inner class for public key details
        public static class KeyConfig {
            private String privateKey;
            private String publicKey;
            private String kid;
            private Duration expiration;

            public KeyConfig() {
            }

            public String getPrivateKey() {
                return this.privateKey;
            }

            public void setPrivateKey(String privateKey) {
                this.privateKey = privateKey;
            }

            public String getPublicKey() {
                return this.publicKey;
            }

            public void setPublicKey(String publicKey) {
                this.publicKey = publicKey;
            }

            public String getKid() {
                return this.kid;
            }

            public void setKid(String kid) {
                this.kid = kid;
            }

            public Duration getExpiration() {
                return this.expiration;
            }

            public void setExpiration(Duration expiration) {
                this.expiration = expiration;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                KeyConfig keyConfig = (KeyConfig) o;
                return Objects.equals(privateKey, keyConfig.privateKey) &&
                        Objects.equals(publicKey, keyConfig.publicKey) &&
                        Objects.equals(kid, keyConfig.kid) &&
                        Objects.equals(expiration, keyConfig.expiration);
            }

            @Override
            public int hashCode() {
                return Objects.hash(privateKey, publicKey, kid, expiration);
            }

            @Override
            public String toString() {
                return "KeyConfig{" +
                        "privateKey=" + (privateKey != null ? "[REDACTED]" : "null") +
                        ", publicKey=" + (publicKey != null ? "[REDACTED]" : "null") +
                        ", kid='" + kid + '\'' +
                        ", expiration=" + expiration +
                        '}';
            }
        }
    }

    // --- Nested class for Local (Symmetric) Keys ---
    public static class Local {
        private KeyConfig access = new KeyConfig();
        private KeyConfig refresh = new KeyConfig();

        public Local() {
        }

        public KeyConfig getAccess() {
            return this.access;
        }

        public void setAccess(KeyConfig access) {
            this.access = access;
        }

        public KeyConfig getRefresh() {
            return this.refresh;
        }

        public void setRefresh(KeyConfig refresh) {
            this.refresh = refresh;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Local local = (Local) o;
            return Objects.equals(access, local.access) && Objects.equals(refresh, local.refresh);
        }

        @Override
        public int hashCode() {
            return Objects.hash(access, refresh);
        }

        @Override
        public String toString() {
            return "Local{" +
                    "access=" + access +
                    ", refresh=" + refresh +
                    '}';
        }

        // Inner class for local key details
        public static class KeyConfig {
            private String secretKey;
            private String kid;
            private Duration expiration;

            public KeyConfig() {
            }

            public String getSecretKey() {
                return this.secretKey;
            }

            public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
            }

            public String getKid() {
                return this.kid;
            }

            public void setKid(String kid) {
                this.kid = kid;
            }

            public Duration getExpiration() {
                return this.expiration;
            }

            public void setExpiration(Duration expiration) {
                this.expiration = expiration;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                KeyConfig keyConfig = (KeyConfig) o;
                return Objects.equals(secretKey, keyConfig.secretKey) &&
                        Objects.equals(kid, keyConfig.kid) &&
                        Objects.equals(expiration, keyConfig.expiration);
            }

            @Override
            public int hashCode() {
                return Objects.hash(secretKey, kid, expiration);
            }

            @Override
            public String toString() {
                return "KeyConfig{" +
                        "secretKey=" + (secretKey != null ? "[REDACTED]" : "null") +
                        ", kid='" + kid + '\'' +
                        ", expiration=" + expiration +
                        '}';
            }
        }
    }
}