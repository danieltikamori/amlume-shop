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

import java.util.Objects;

/**
 * Configuration properties for PASETO token generation and validation.
 * Maps properties under the 'paseto' prefix in application configuration files (e.g., application.yml).
 * <p>
 * Example YAML structure:
 * <pre>{@code
 * paseto:
 *   token-no-footer-parts: 3
 *   token-with-footer-parts: 4
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

@Configuration
@ConfigurationProperties(prefix = "paseto")
public class PasetoProperties {

    private int tokenNoFooterParts;
    private int tokenWithFooterParts;
    private Access access = new Access(); // Initialize nested objects
    private Refresh refresh = new Refresh();

    public PasetoProperties() {
    }

    // --- Standard Getters/Setters for direct properties and nested objects ---
    public int getTokenNoFooterParts() { return this.tokenNoFooterParts; }
    public void setTokenNoFooterParts(int tokenNoFooterParts) { this.tokenNoFooterParts = tokenNoFooterParts; }

    public int getTokenWithFooterParts() { return this.tokenWithFooterParts; }
    public void setTokenWithFooterParts(int tokenWithFooterParts) { this.tokenWithFooterParts = tokenWithFooterParts; }

    public Access getAccess() { return this.access; }
    public void setAccess(Access access) { this.access = access; }

    public Refresh getRefresh() { return this.refresh; }
    public void setRefresh(Refresh refresh) { this.refresh = refresh; }

    // --- equals, hashCode, toString (Generated or standard) ---
    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PasetoProperties other)) return false;
        if (this.getTokenNoFooterParts() != other.getTokenNoFooterParts()) return false;
        if (this.getTokenWithFooterParts() != other.getTokenWithFooterParts()) return false;
        final Object this$access = this.getAccess();
        final Object other$access = other.getAccess();
        if (!Objects.equals(this$access, other$access)) return false;
        final Object this$refresh = this.getRefresh();
        final Object other$refresh = other.getRefresh();
        return Objects.equals(this$refresh, other$refresh);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getTokenNoFooterParts();
        result = result * PRIME + this.getTokenWithFooterParts();
        final Object $access = this.getAccess();
        result = result * PRIME + ($access == null ? 43 : $access.hashCode());
        final Object $refresh = this.getRefresh();
        result = result * PRIME + ($refresh == null ? 43 : $refresh.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "PasetoProperties(tokenNoFooterParts=" + this.getTokenNoFooterParts() + ", tokenWithFooterParts=" + this.getTokenWithFooterParts() + ", access=" + this.getAccess() + ", refresh=" + this.getRefresh() + ")";
    }


    // --- Nested classes for access and refresh properties ---
    public static class Access {
        private long expiration;
        private Local local = new Local();
        private Public pub = new Public();

        public Access() { }

        public long getExpiration() { return this.expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }

        public Local getLocal() { return this.local; }
        public void setLocal(Local local) { this.local = local; }

        public Public getPub() { return this.pub; }
        public void setPub(Public pub) { this.pub = pub; }

        // equals, hashCode, toString for Access
        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof Access other)) return false;
            if (this.getExpiration() != other.getExpiration()) return false;
            final Object this$local = this.getLocal();
            final Object other$local = other.getLocal();
            if (!Objects.equals(this$local, other$local)) return false;
            final Object this$pub = this.getPub();
            final Object other$pub = other.getPub();
            return Objects.equals(this$pub, other$pub);
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $expiration = this.getExpiration();
            result = result * PRIME + Long.hashCode($expiration);
            final Object $local = this.getLocal();
            result = result * PRIME + ($local == null ? 43 : $local.hashCode());
            final Object $pub = this.getPub();
            result = result * PRIME + ($pub == null ? 43 : $pub.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "PasetoProperties.Access(expiration=" + this.getExpiration() + ", local=" + this.getLocal() + ", pub=" + this.getPub() + ")";
        }


        public static class Local {
            private String secretKey;
            private String kid;
            private Long expiration; // Optional override

            public Local() { }

            public String getSecretKey() { return this.secretKey; }
            public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

            public String getKid() { return this.kid; }
            public void setKid(String kid) { this.kid = kid; }

            public Long getExpiration() { return this.expiration; }
            public void setExpiration(Long expiration) { this.expiration = expiration; }

            // equals, hashCode, toString for Access.Local
            @Override
            public boolean equals(final Object o) {
                if (o == this) return true;
                if (!(o instanceof Local other)) return false;
                final Object this$secretKey = this.getSecretKey();
                final Object other$secretKey = other.getSecretKey();
                if (!Objects.equals(this$secretKey, other$secretKey)) return false;
                final Object this$kid = this.getKid();
                final Object other$kid = other.getKid();
                if (!Objects.equals(this$kid, other$kid)) return false;
                final Object this$expiration = this.getExpiration();
                final Object other$expiration = other.getExpiration();
                return Objects.equals(this$expiration, other$expiration);
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $secretKey = this.getSecretKey();
                result = result * PRIME + ($secretKey == null ? 43 : $secretKey.hashCode());
                final Object $kid = this.getKid();
                result = result * PRIME + ($kid == null ? 43 : $kid.hashCode());
                final Object $expiration = this.getExpiration();
                result = result * PRIME + ($expiration == null ? 43 : $expiration.hashCode());
                return result;
            }

            @Override
            public String toString() {
                return "PasetoProperties.Access.Local(secretKey=" + this.getSecretKey() + ", kid=" + this.getKid() + ", expiration=" + this.getExpiration() + ")";
            }
        }

        public static class Public {
            private String privateKey;
            private String publicKey;
            private String kid;
            private Long expiration; // Optional override

            public Public() { }

            public String getPrivateKey() { return this.privateKey; }
            public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

            public String getPublicKey() { return this.publicKey; }
            public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

            public String getKid() { return this.kid; }
            public void setKid(String kid) { this.kid = kid; }

            public Long getExpiration() { return this.expiration; }
            public void setExpiration(Long expiration) { this.expiration = expiration; }

            // equals, hashCode, toString for Access.Public
            @Override
            public boolean equals(final Object o) {
                if (o == this) return true;
                if (!(o instanceof Public other)) return false;
                final Object this$privateKey = this.getPrivateKey();
                final Object other$privateKey = other.getPrivateKey();
                if (!Objects.equals(this$privateKey, other$privateKey)) return false;
                final Object this$publicKey = this.getPublicKey();
                final Object other$publicKey = other.getPublicKey();
                if (!Objects.equals(this$publicKey, other$publicKey)) return false;
                final Object this$kid = this.getKid();
                final Object other$kid = other.getKid();
                if (!Objects.equals(this$kid, other$kid)) return false;
                final Object this$expiration = this.getExpiration();
                final Object other$expiration = other.getExpiration();
                return Objects.equals(this$expiration, other$expiration);
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $privateKey = this.getPrivateKey();
                result = result * PRIME + ($privateKey == null ? 43 : $privateKey.hashCode());
                final Object $publicKey = this.getPublicKey();
                result = result * PRIME + ($publicKey == null ? 43 : $publicKey.hashCode());
                final Object $kid = this.getKid();
                result = result * PRIME + ($kid == null ? 43 : $kid.hashCode());
                final Object $expiration = this.getExpiration();
                result = result * PRIME + ($expiration == null ? 43 : $expiration.hashCode());
                return result;
            }

            @Override
            public String toString() {
                return "PasetoProperties.Access.Public(privateKey=" + this.getPrivateKey() + ", publicKey=" + this.getPublicKey() + ", kid=" + this.getKid() + ", expiration=" + this.getExpiration() + ")";
            }
        }

        // --- REMOVED Getters for easier access that were inside Access class ---
        // public String getAccessPrivateKey() { return pub != null ? pub.getPrivateKey() : null; }
        // public String getAccessPublicKey() { return pub != null ? pub.getPublicKey() : null; }
        // public String getAccessSecretKey() { return local != null ? local.getSecretKey() : null; }
    }

    public static class Refresh {
        private long expiration;
        private Local local = new Local();
        private Public pub = new Public();

        public Refresh() { }

        public long getExpiration() { return this.expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }

        public Local getLocal() { return this.local; }
        public void setLocal(Local local) { this.local = local; }

        public Public getPub() { return this.pub; }
        public void setPub(Public pub) { this.pub = pub; }

        // equals, hashCode, toString for Refresh
        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof Refresh other)) return false;
            if (this.getExpiration() != other.getExpiration()) return false;
            final Object this$local = this.getLocal();
            final Object other$local = other.getLocal();
            if (!Objects.equals(this$local, other$local)) return false;
            final Object this$pub = this.getPub();
            final Object other$pub = other.getPub();
            return Objects.equals(this$pub, other$pub);
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $expiration = this.getExpiration();
            result = result * PRIME + Long.hashCode($expiration);
            final Object $local = this.getLocal();
            result = result * PRIME + ($local == null ? 43 : $local.hashCode());
            final Object $pub = this.getPub();
            result = result * PRIME + ($pub == null ? 43 : $pub.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "PasetoProperties.Refresh(expiration=" + this.getExpiration() + ", local=" + this.getLocal() + ", pub=" + this.getPub() + ")";
        }

        public static class Local {
            private String secretKey;
            private String kid;
            private Long expiration; // Optional override

            public Local() { }

            public String getSecretKey() { return this.secretKey; }
            public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

            public String getKid() { return this.kid; }
            public void setKid(String kid) { this.kid = kid; }

            public Long getExpiration() { return this.expiration; }
            public void setExpiration(Long expiration) { this.expiration = expiration; }

            // equals, hashCode, toString for Refresh.Local
            @Override
            public boolean equals(final Object o) {
                if (o == this) return true;
                if (!(o instanceof Local other)) return false;
                final Object this$secretKey = this.getSecretKey();
                final Object other$secretKey = other.getSecretKey();
                if (!Objects.equals(this$secretKey, other$secretKey)) return false;
                final Object this$kid = this.getKid();
                final Object other$kid = other.getKid();
                if (!Objects.equals(this$kid, other$kid)) return false;
                final Object this$expiration = this.getExpiration();
                final Object other$expiration = other.getExpiration();
                return Objects.equals(this$expiration, other$expiration);
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $secretKey = this.getSecretKey();
                result = result * PRIME + ($secretKey == null ? 43 : $secretKey.hashCode());
                final Object $kid = this.getKid();
                result = result * PRIME + ($kid == null ? 43 : $kid.hashCode());
                final Object $expiration = this.getExpiration();
                result = result * PRIME + ($expiration == null ? 43 : $expiration.hashCode());
                return result;
            }

            @Override
            public String toString() {
                return "PasetoProperties.Refresh.Local(secretKey=" + this.getSecretKey() + ", kid=" + this.getKid() + ", expiration=" + this.getExpiration() + ")";
            }
        }

        public static class Public {
            private String privateKey;
            private String publicKey;
            private String kid;
            private Long expiration; // Optional override

            public Public() { }

            public String getPrivateKey() { return this.privateKey; }
            public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

            public String getPublicKey() { return this.publicKey; }
            public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

            public String getKid() { return this.kid; }
            public void setKid(String kid) { this.kid = kid; }

            public Long getExpiration() { return this.expiration; }
            public void setExpiration(Long expiration) { this.expiration = expiration; }

            // equals, hashCode, toString for Refresh.Public
            @Override
            public boolean equals(final Object o) {
                if (o == this) return true;
                if (!(o instanceof Public other)) return false;
                final Object this$privateKey = this.getPrivateKey();
                final Object other$privateKey = other.getPrivateKey();
                if (!Objects.equals(this$privateKey, other$privateKey)) return false;
                final Object this$publicKey = this.getPublicKey();
                final Object other$publicKey = other.getPublicKey();
                if (!Objects.equals(this$publicKey, other$publicKey)) return false;
                final Object this$kid = this.getKid();
                final Object other$kid = other.getKid();
                if (!Objects.equals(this$kid, other$kid)) return false;
                final Object this$expiration = this.getExpiration();
                final Object other$expiration = other.getExpiration();
                return Objects.equals(this$expiration, other$expiration);
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $privateKey = this.getPrivateKey();
                result = result * PRIME + ($privateKey == null ? 43 : $privateKey.hashCode());
                final Object $publicKey = this.getPublicKey();
                result = result * PRIME + ($publicKey == null ? 43 : $publicKey.hashCode());
                final Object $kid = this.getKid();
                result = result * PRIME + ($kid == null ? 43 : $kid.hashCode());
                final Object $expiration = this.getExpiration();
                result = result * PRIME + ($expiration == null ? 43 : $expiration.hashCode());
                return result;
            }

            @Override
            public String toString() {
                return "PasetoProperties.Refresh.Public(privateKey=" + this.getPrivateKey() + ", publicKey=" + this.getPublicKey() + ", kid=" + this.getKid() + ", expiration=" + this.getExpiration() + ")";
            }
        }

        // --- REMOVED Getters for easier access that were inside Refresh class ---
        // public String getRefreshPrivateKey() { return pub != null ? pub.getPrivateKey() : null; }
        // public String getRefreshPublicKey() { return pub != null ? pub.getPublicKey() : null; }
        // public String getRefreshSecretKey() { return local != null ? local.getSecretKey() : null; }
    }

    // --- REMOVED ALL top-level getters that delegate to nested classes ---
    // These were causing the binding conflict
    // public String getAccessPublicKid() { ... }
    // public String getAccessLocalKid() { ... }
    // public String getRefreshPublicKid() { ... }
    // public String getRefreshLocalKid() { ... }
}