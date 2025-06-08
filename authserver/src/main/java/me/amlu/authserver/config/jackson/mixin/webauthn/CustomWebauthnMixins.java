/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config.jackson.mixin.webauthn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.api.CredProtectAuthenticationExtensionsClientInput.CredProtect;
import org.springframework.security.web.webauthn.api.CredProtectAuthenticationExtensionsClientInput.CredProtect.ProtectionPolicy;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings({"unused"})
public class CustomWebauthnMixins {
    /**
     * Custom Jackson mixins for WebAuthn API objects from Spring Security.
     * These mixins are used to configure how Jackson serializes and deserializes
     * these objects, specifically by adding the {@code @class} property for
     * type information, which is necessary for polymorphic deserialization.
     */
    /**
     * @see org.springframework.security.web.webauthn.api.Bytes
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class WebauthnBytesMixIn {
        @JsonCreator
        public WebauthnBytesMixIn(
                @JsonProperty("bytes") byte[] bytes
        ) {
            Objects.requireNonNull(bytes, "bytes cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class PublicKeyCredentialCreationOptionsMixIn {

        @JsonCreator
        public PublicKeyCredentialCreationOptionsMixIn(
                @JsonProperty("rp") PublicKeyCredentialRpEntity rp,
                @JsonProperty("user") PublicKeyCredentialUserEntity user,
                @JsonProperty("challenge") Bytes challenge,
                @JsonProperty("pubKeyCredParams") List<PublicKeyCredentialParameters> pubKeyCredParams,
                @JsonProperty("timeout") Duration timeout,
                @JsonProperty("excludeCredentials") List<PublicKeyCredentialDescriptor> excludeCredentials,
                @JsonProperty("authenticatorSelection") AuthenticatorSelectionCriteria authenticatorSelection,
                @JsonProperty("attestation") AttestationConveyancePreference attestation,
                @JsonProperty("extensions") AuthenticationExtensionsClientInputs extensions
        ) {
            Objects.requireNonNull(rp, "rp cannot be null");
            Objects.requireNonNull(user, "user cannot be null");
            Objects.requireNonNull(challenge, "challenge cannot be null");
            Objects.requireNonNull(pubKeyCredParams, "pubKeyCredParams cannot be null");
            // timeout can be null
            // excludeCredentials, authenticatorSelection, attestation, extensions can be null
        }
    }

    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
            builder = org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions.PublicKeyCredentialCreationOptionsBuilder.class
    )
    public abstract static class PublicKeyCredentialCreationOptionsDeserializationMixin implements Serializable {
        // This annotation tells Jackson to use the specified builder class for deserialization.
        // Jackson will then try to match JSON properties to methods on this builder.
        // The builder's build() method will be called to construct the final object.
        // Note: The actual builder class is PublicKeyCredentialCreationOptions.PublicKeyCredentialCreationOptionsBuilder

//        private static final long serialVersionUID = 1L; // Add if the target class is Serializable and you want to control versioning
    }

    /**
     * @see org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class PublicKeyCredentialRpEntityMixIn {
        @JsonCreator
        public PublicKeyCredentialRpEntityMixIn(
                @JsonProperty("name") String name,
                @JsonProperty("id") String id
        ) {
            Objects.requireNonNull(name, "name cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class PublicKeyCredentialUserEntityMixIn {
        @JsonCreator
        public PublicKeyCredentialUserEntityMixIn(
                @JsonProperty("name") String name,
                @JsonProperty("id") Bytes id,
                @JsonProperty("displayName") String displayName
        ) {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(displayName, "displayName cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class PublicKeyCredentialParametersMixIn {
        @JsonCreator
        public PublicKeyCredentialParametersMixIn(
                @JsonProperty("type") PublicKeyCredentialType type,
                @JsonProperty("alg") COSEAlgorithmIdentifier alg
        ) {
            Objects.requireNonNull(type, "type cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AuthenticatorSelectionCriteriaMixIn {
        @JsonCreator
        public AuthenticatorSelectionCriteriaMixIn(
                @JsonProperty("authenticatorAttachment") AuthenticatorAttachment authenticatorAttachment,
                @JsonProperty("residentKey") ResidentKeyRequirement residentKey,
                @JsonProperty("userVerification") UserVerificationRequirement userVerification,
                @JsonProperty("requireResidentKey") Boolean requireResidentKey // Older field, keep for compatibility if necessary
        ) {
            // authenticatorAttachment, residentKey, userVerification can be null
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.AttestationConveyancePreference
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AttestationConveyancePreferenceMixIn {
        @JsonCreator
        public AttestationConveyancePreferenceMixIn(
                @JsonProperty("value") String value
        ) {
            Objects.requireNonNull(value, "value cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.ResidentKeyRequirement
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class ResidentKeyRequirementMixIn {
        @JsonCreator
        public ResidentKeyRequirementMixIn(
                @JsonProperty("value") String value
        ) {
            Objects.requireNonNull(value, "value cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.UserVerificationRequirement
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class UserVerificationRequirementMixIn {
        @JsonCreator
        public UserVerificationRequirementMixIn(
                @JsonProperty("value") String value
        ) {
            Objects.requireNonNull(value, "value cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class PublicKeyCredentialRequestOptionsMixIn {
        @JsonCreator
        public PublicKeyCredentialRequestOptionsMixIn(
                @JsonProperty("challenge") Bytes challenge,
                @JsonProperty("timeout") Duration timeout,
                @JsonProperty("rpId") String rpId,
                @JsonProperty("allowCredentials") List<PublicKeyCredentialDescriptor> allowCredentials,
                @JsonProperty("userVerification") UserVerificationRequirement userVerification,
                @JsonProperty("extensions") AuthenticationExtensionsClientInputs extensions
        ) {
            Objects.requireNonNull(challenge, "challenge cannot be null");
            // timeout, rpId, allowCredentials, userVerification, extensions can be null
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.AuthenticatorAttachment
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AuthenticatorAttachmentMixIn {
        @JsonCreator
        public AuthenticatorAttachmentMixIn(
                @JsonProperty("value") String value
        ) {
            Objects.requireNonNull(value, "value cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInputs
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AuthenticationExtensionsClientInputsMixIn {
        // If ImmutableAuthenticationExtensionsClientInputs is the concrete class,
        // and it has a suitable constructor or factory method, you can add a @JsonCreator here.
        // Often, for interfaces/abstract classes, Jackson relies on @JsonTypeInfo or specific deserializers.
        // If it's truly immutable with no public constructor/factory Jackson can use,
        // this might remain problematic without a custom deserializer.
        // However, Spring's WebauthnJackson2Module should provide one.
        // This mixin is more about guiding if needed, or if you use a custom concrete class.
        @JsonCreator
        public AuthenticationExtensionsClientInputsMixIn(
                @JsonProperty("inputs") List<?> inputs
        ) {
            // inputs can be null
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInput
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AuthenticationExtensionsClientInputMixIn {
        @JsonCreator
        public AuthenticationExtensionsClientInputMixIn(
                @JsonProperty("extensionId") String extensionId,
                @JsonProperty("input") String input
        ) {
            Objects.requireNonNull(extensionId, "extensionId cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class PublicKeyCredentialDescriptorMixIn {
        @JsonCreator
        public PublicKeyCredentialDescriptorMixIn(
                @JsonProperty("type") PublicKeyCredentialType type,
                @JsonProperty("id") Bytes id,
                @JsonProperty("transports") Set<AuthenticatorTransport> transports
        ) {
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(id, "id cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.PublicKeyCredentialType
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class PublicKeyCredentialTypeMixIn {
        @JsonCreator
        public PublicKeyCredentialTypeMixIn(
                @JsonProperty("value") String value
        ) {
            Objects.requireNonNull(value, "value cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.COSEAlgorithmIdentifier
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class COSEAlgorithmIdentifierMixIn {
        @JsonCreator
        public COSEAlgorithmIdentifierMixIn(
                @JsonProperty("value") long value
        ) {
            // value is a primitive, no null check needed
        }
    }


    /**
     * @see org.springframework.security.web.webauthn.api.AuthenticatorTransport
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AuthenticatorTransportMixIn {
        @JsonCreator
        public AuthenticatorTransportMixIn(
                @JsonProperty("value") String value
        ) {
            Objects.requireNonNull(value, "value cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.CredProtectAuthenticationExtensionsClientInput
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class CredProtectAuthenticationExtensionsClientInputMixIn {
        @JsonCreator
        public CredProtectAuthenticationExtensionsClientInputMixIn(
                @JsonProperty("input") CredProtect input
        ) {
            Objects.requireNonNull(input, "input cannot be null");
        }

        @JsonIgnore
        public abstract String getExtensionId();
    }

    /**
     * @see org.springframework.security.web.webauthn.api.CredProtectAuthenticationExtensionsClientInput.CredProtect
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class CredProtectMixIn {
        @JsonCreator
        public CredProtectMixIn(
                @JsonProperty("credProtectionPolicy") ProtectionPolicy credProtectionPolicy,
                @JsonProperty("enforceCredentialProtectionPolicy") boolean enforceCredentialProtectionPolicy
        ) {
            Objects.requireNonNull(credProtectionPolicy, "credProtectionPolicy cannot be null");
        }
    }

    // MIXIN for org.springframework.security.web.webauthn.api.Bytes
    public static abstract class BytesMixIn {
        /**
         * Creator to handle deserialization of Bytes when it's represented in JSON
         * as an object like {"bytes": "base64encodedstring"}.
         * This structure appears in your session data for the 'challenge'.
         */
        @JsonCreator
        public static Bytes fromObjectWithBytesProperty(@JsonProperty("bytes") String base64Value) {
            if (base64Value == null) {
                // Or throw new IllegalArgumentException("Missing 'bytes' field for Bytes deserialization");
                // Depending on whether a null challenge is ever valid. For WebAuthn, challenge is typically non-null.
//                return null;
                throw new IllegalArgumentException("Missing 'bytes' field for Bytes deserialization");

            }
            return Bytes.fromBase64(base64Value);
        }

        // It's also good practice to ensure serialization uses toBase64() if not already handled.
        // The WebauthnJackson2Module's BytesSerializer should do this, but @JsonValue provides a strong hint.
        // @com.fasterxml.jackson.annotation.JsonValue
        // public abstract String toBase64(); // Or toBase64Url() depending on Spring Security's serializer's choice
    }
}
