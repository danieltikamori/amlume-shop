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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.amlu.authserver.config.jackson.serializer.DurationDeserializer;
import me.amlu.authserver.config.jackson.serializer.DurationSerializer;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.api.CredProtectAuthenticationExtensionsClientInput.CredProtect;
import org.springframework.security.web.webauthn.api.CredProtectAuthenticationExtensionsClientInput.CredProtect.ProtectionPolicy;

import java.io.Serial;
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
    /**
     * For org.springframework.security.web.webauthn.api.Bytes
     * This mixin ensures that when Bytes objects are serialized/deserialized
     * with default typing enabled (e.g., in session), the @class type information
     * is included. The actual serialization to a Base64URL string and deserialization
     * from it is handled by Spring Security's WebauthnJackson2Module via @JsonValue
     * on the Bytes class and its BytesDeserializer.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class WebauthnBytesMixIn {
        // No @JsonCreator here.
        // Jackson, with default typing, will use WebauthnJackson2Module's BytesSerializer (outputs string)
        // and BytesDeserializer (expects string for the value part).
        // The @JsonValue on Bytes.toBase64UrlString() is key.

//        @JsonCreator
//        public WebauthnBytesMixIn(
//                @JsonProperty("bytes") byte[] bytes
//        ) {
//            Objects.requireNonNull(bytes, "bytes cannot be null");
//        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class PublicKeyCredentialCreationOptionsMixIn {

        // Ensure this uses the string-based DurationSerializer that handles serializeWithType
        @JsonSerialize(using = DurationSerializer.class)
        public abstract Duration getTimeout();

        // Optional: if we also need custom deserialization for this field
        @JsonDeserialize(using = DurationDeserializer.class)
        public abstract void setTimeout(Duration timeout);

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
        @Serial
        private static final long serialVersionUID = 1L;
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
            Objects.requireNonNull(alg, "alg cannot be null"); // alg is also non-null in the target class constructor
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AuthenticatorSelectionCriteriaMixIn {
        // Consider removing @JsonCreator if WebauthnJackson2Module handles deserialization well.
        // The main purpose here is @JsonTypeInfo.
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
        // Empty mixin - just adding the @JsonTypeInfo annotation
        // The concrete implementation will be handled by WebauthnJackson2Module
    }

    /**
     * @see org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInput
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AuthenticationExtensionsClientInputMixIn {
        // This is for an interface. Deserialization typically needs a concrete type or custom deserializer.

        @JsonCreator
        public AuthenticationExtensionsClientInputMixIn(
                @JsonProperty("extensionId") String extensionId,
                @JsonProperty("input") String input
        ) {
            Objects.requireNonNull(extensionId, "extensionId cannot be null");
        }
    }

    /**
     * @see org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientOutputs
     * This mixin is for the interface.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class AuthenticationExtensionsClientOutputsMixIn {

        // Empty mixin - just adding the @JsonTypeInfo annotation
        // The concrete implementation will be handled by WebauthnJackson2Module

        // No explicit @JsonCreator needed if Jackson can find a way to instantiate
        // a concrete type (like ImmutableAuthenticationExtensionsClientOutputs)
        // based on the @class property and the PTV.
        // The WebauthnJackson2Module provides a deserializer for the interface,
        // which should be used when @class is not present or when default typing is off.
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
                @JsonProperty("credProtectionPolicy") CredProtect.ProtectionPolicy credProtectionPolicy,
                @JsonProperty("enforceCredentialProtectionPolicy") boolean enforceCredentialProtectionPolicy
        ) {
            Objects.requireNonNull(credProtectionPolicy, "credProtectionPolicy cannot be null");
        }
    }

    // MIXIN for org.springframework.security.web.webauthn.api.Bytes
//    public static abstract class BytesMixIn {
//        /**
//         * Creator to handle deserialization of Bytes when it's represented in JSON
//         * as an object like {"bytes": "base64encodedstring"}.
//         * This structure appears in your session data for the 'challenge'.
//         */
//        @JsonCreator
//        public static Bytes fromObjectWithBytesProperty(@JsonProperty("bytes") String base64Value) {
//            if (base64Value == null) {
//                // Or throw new IllegalArgumentException("Missing 'bytes' field for Bytes deserialization");
//                // Depending on whether a null challenge is ever valid. For WebAuthn, challenge is typically non-null.
////                return null;
//                throw new IllegalArgumentException("Missing 'bytes' field for Bytes deserialization");
//
//            }
//            return Bytes.fromBase64(base64Value);
//        }
//
//        // It's also good practice to ensure serialization uses toBase64() if not already handled.
//        // The WebauthnJackson2Module's BytesSerializer should do this, but @JsonValue provides a strong hint.
//        // @com.fasterxml.jackson.annotation.JsonValue
//        // public abstract String toBase64(); // Or toBase64Url() depending on Spring Security's serializer's choice
//    }
}
