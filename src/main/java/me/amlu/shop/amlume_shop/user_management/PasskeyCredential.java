/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "passkey_credentials", indexes = {
        @Index(name = "idx_passkey_credential_id_unique", columnList = "credential_id", unique = true),
        @Index(name = "idx_passkey_user_id", columnList = "user_id")
})
public class PasskeyCredential extends BaseEntity { // Extend BaseEntity if you want auditing

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passkey_id")
    private Long id; // Internal DB ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    // Store as Base64URL encoded String for easier handling (DB/JSON)
    @NotBlank
    @Column(name = "credential_id", nullable = false, unique = true, length = 512) // Adjust length as needed
    private String credentialId; // The unique ID from the authenticator

    // Store as Base64URL encoded String (COSE format)
    @NotBlank
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT") // Use TEXT for potentially long keys
    private String publicKey;

    @NotNull
    @PositiveOrZero
    @Column(name = "signature_counter", nullable = false)
    private Long signatureCounter;

    // Store as Base64URL encoded String
    @NotBlank
    @Column(name = "user_handle", nullable = false, length = 128) // User handle provided during registration
    private String userHandle;

    @Column(name = "display_name", length = 255) // User-friendly name for the key
    private String displayName;

    @Column(name = "aaguid", length = 36) // Authenticator Attestation GUID (UUID format)
    private String aaguid;

    @ElementCollection(fetch = FetchType.EAGER) // Eager might be okay for small sets like transports
    @CollectionTable(name = "passkey_transports", joinColumns = @JoinColumn(name = "passkey_id"))
    @Column(name = "transport")
    private Set<String> transports = new HashSet<>(); // e.g., "internal", "usb", "nfc", "ble"

    @Column(name = "creation_timestamp", nullable = false, updatable = false)
    @CreationTimestamp // Use Hibernate annotation if available
    private Instant creationTimestamp;

    @Column(name = "last_used_timestamp")
    @UpdateTimestamp // Use Hibernate annotation if available
    private Instant lastUsedTimestamp;

    public PasskeyCredential(Long id, @NotNull User user, @NotBlank String credentialId, @NotBlank String publicKey, @NotNull @PositiveOrZero Long signatureCounter, @NotBlank String userHandle, String displayName, String aaguid, Set<String> transports, Instant creationTimestamp, Instant lastUsedTimestamp) {
        this.id = id;
        this.user = user;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.signatureCounter = signatureCounter;
        this.userHandle = userHandle;
        this.displayName = displayName;
        this.aaguid = aaguid;
        this.transports = transports;
        this.creationTimestamp = creationTimestamp;
        this.lastUsedTimestamp = lastUsedTimestamp;
    }

    public PasskeyCredential() {
    }

    public static PasskeyCredentialBuilder builder() {
        return new PasskeyCredentialBuilder();
    }

    // --- Auditable Implementation (if extending BaseEntity) ---
    @Override
    @Transient
    public Long getAuditableId() {
        return this.id;
    }
    // --- End Auditable ---

    // --- equals() and hashCode() based on credentialId (which should be unique) ---
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PasskeyCredential that = (PasskeyCredential) o;
        // Use credentialId for equality check as it's the business key from the authenticator
        return getCredentialId() != null && Objects.equals(getCredentialId(), that.getCredentialId());
    }

    @Override
    public final int hashCode() {
        // Use credentialId for hash code
        return Objects.hash(credentialId);
    }
    // --- End equals() and hashCode() ---

    @Override
    public String toString() {
        return "PasskeyCredential(" +
                "id=" + id +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", credentialId='" + credentialId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", signatureCounter=" + signatureCounter +
                ", transports=" + transports +
                ", creationTimestamp=" + creationTimestamp +
                ", lastUsedTimestamp=" + lastUsedTimestamp +
                ')';
    }

    public Long getId() {
        return this.id;
    }

    public @NotNull User getUser() {
        return this.user;
    }

    public @NotBlank String getCredentialId() {
        return this.credentialId;
    }

    public @NotBlank String getPublicKey() {
        return this.publicKey;
    }

    public @NotNull @PositiveOrZero Long getSignatureCounter() {
        return this.signatureCounter;
    }

    public @NotBlank String getUserHandle() {
        return this.userHandle;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getAaguid() {
        return this.aaguid;
    }

    public Set<String> getTransports() {
        return this.transports;
    }

    public Instant getCreationTimestamp() {
        return this.creationTimestamp;
    }

    public Instant getLastUsedTimestamp() {
        return this.lastUsedTimestamp;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(@NotNull User user) {
        this.user = user;
    }

    public void setCredentialId(@NotBlank String credentialId) {
        this.credentialId = credentialId;
    }

    public void setPublicKey(@NotBlank String publicKey) {
        this.publicKey = publicKey;
    }

    public void setSignatureCounter(@NotNull @PositiveOrZero Long signatureCounter) {
        this.signatureCounter = signatureCounter;
    }

    public void setUserHandle(@NotBlank String userHandle) {
        this.userHandle = userHandle;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setAaguid(String aaguid) {
        this.aaguid = aaguid;
    }

    public void setTransports(Set<String> transports) {
        this.transports = transports;
    }

    public void setCreationTimestamp(Instant creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public void setLastUsedTimestamp(Instant lastUsedTimestamp) {
        this.lastUsedTimestamp = lastUsedTimestamp;
    }

    public static class PasskeyCredentialBuilder {
        private Long id;
        private @NotNull User user;
        private @NotBlank String credentialId;
        private @NotBlank String publicKey;
        private @NotNull
        @PositiveOrZero Long signatureCounter;
        private @NotBlank String userHandle;
        private String displayName;
        private String aaguid;
        private Set<String> transports;
        private Instant creationTimestamp;
        private Instant lastUsedTimestamp;

        PasskeyCredentialBuilder() {
        }

        public PasskeyCredentialBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PasskeyCredentialBuilder user(@NotNull User user) {
            this.user = user;
            return this;
        }

        public PasskeyCredentialBuilder credentialId(@NotBlank String credentialId) {
            this.credentialId = credentialId;
            return this;
        }

        public PasskeyCredentialBuilder publicKey(@NotBlank String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public PasskeyCredentialBuilder signatureCounter(@NotNull @PositiveOrZero Long signatureCounter) {
            this.signatureCounter = signatureCounter;
            return this;
        }

        public PasskeyCredentialBuilder userHandle(@NotBlank String userHandle) {
            this.userHandle = userHandle;
            return this;
        }

        public PasskeyCredentialBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public PasskeyCredentialBuilder aaguid(String aaguid) {
            this.aaguid = aaguid;
            return this;
        }

        public PasskeyCredentialBuilder transports(Set<String> transports) {
            this.transports = transports;
            return this;
        }

        public PasskeyCredentialBuilder creationTimestamp(Instant creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public PasskeyCredentialBuilder lastUsedTimestamp(Instant lastUsedTimestamp) {
            this.lastUsedTimestamp = lastUsedTimestamp;
            return this;
        }

        public PasskeyCredential build() {
            return new PasskeyCredential(this.id, this.user, this.credentialId, this.publicKey, this.signatureCounter, this.userHandle, this.displayName, this.aaguid, this.transports, this.creationTimestamp, this.lastUsedTimestamp);
        }

        public String toString() {
            return "PasskeyCredential.PasskeyCredentialBuilder(id=" + this.id + ", user=" + this.user + ", credentialId=" + this.credentialId + ", publicKey=" + this.publicKey + ", signatureCounter=" + this.signatureCounter + ", userHandle=" + this.userHandle + ", displayName=" + this.displayName + ", aaguid=" + this.aaguid + ", transports=" + this.transports + ", creationTimestamp=" + this.creationTimestamp + ", lastUsedTimestamp=" + this.lastUsedTimestamp + ")";
        }
    }
}
