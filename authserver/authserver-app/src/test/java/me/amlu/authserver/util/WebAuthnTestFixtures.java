/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.webauthn.api.*;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class WebAuthnTestFixtures {

    // Default values that can be overridden by parameters
    private static final String DEFAULT_RP_ID = "localhost";
    private static final String DEFAULT_RP_NAME = "Amlume Passkeys";
    private static final String DEFAULT_USERNAME = "testuser@example.com";
    private static final String DEFAULT_USER_DISPLAY_NAME = "Test User";


    public static UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken() {
        return UsernamePasswordAuthenticationToken.authenticated(
                "admin1", "password", List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
    }

    // Overloaded method with defaults
    public static PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions() {
        return publicKeyCredentialCreationOptions(
                DEFAULT_RP_ID,
                DEFAULT_RP_NAME,
                DEFAULT_USERNAME,
                DEFAULT_USER_DISPLAY_NAME,
                Bytes.random() // Default random user ID
        );
    }

    // Configurable method
    public static PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions(
            String rpId, String rpName, String username, String userDisplayName, Bytes userId) {
        log.debug("Creating PublicKeyCredentialCreationOptions with rpId: {}, rpName: {}, username: {}, userDisplayName: {}, userId (handle): {}",
                rpId, rpName, username, userDisplayName, userId.toBase64UrlString());
        return PublicKeyCredentialCreationOptions.builder()
                .rp(PublicKeyCredentialRpEntity.builder().id(rpId).name(rpName).build())
                .user(ImmutablePublicKeyCredentialUserEntity.builder().name(username).id(userId).displayName(userDisplayName).build())
                .challenge(Bytes.random()) // Challenge should always be random for security
                .pubKeyCredParams(List.of(
                        PublicKeyCredentialParameters.ES256, // Common and strong
                        PublicKeyCredentialParameters.RS256, // Widely supported
                        PublicKeyCredentialParameters.ES384,
                        PublicKeyCredentialParameters.EdDSA
                        // PublicKeyCredentialParameters.RS512 // Less common for WebAuthn but available
                ))
                .timeout(Duration.ofSeconds(60))
                .excludeCredentials(Collections.singletonList( // Example: exclude one known credential
                        PublicKeyCredentialDescriptor.builder()
                                .id(Bytes.random()) // This would typically be a known credential ID
                                .type(PublicKeyCredentialType.PUBLIC_KEY)
                                .transports(Set.of(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID))
                                .build()
                ))
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .userVerification(UserVerificationRequirement.PREFERRED)
                        .residentKey(ResidentKeyRequirement.PREFERRED) // PREFERRED is often better than REQUIRED for broader compatibility
                        .authenticatorAttachment(null) // Allow any attachment (platform or cross-platform) for flexibility
                        .build()
                )
                .attestation(AttestationConveyancePreference.NONE) // NONE is common for login, DIRECT/INDIRECT for registration if attestation is needed
                .extensions(
                        new ImmutableAuthenticationExtensionsClientInputs(
                                // Example extension, often not needed for basic tests
                                // new CredProtectAuthenticationExtensionsClientInput(new CredProtect(ProtectionPolicy.USER_VERIFICATION_OPTIONAL_WITH_CREDENTIAL_ID_LIST, true))
                        )
                )
                .build();
    }

    // Overloaded method with defaults
    public static PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions() {
        return publicKeyCredentialRequestOptions(DEFAULT_RP_ID);
    }

    // Configurable method
    public static PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions(String rpId) {
        log.debug("Creating PublicKeyCredentialRequestOptions with rpId: {}", rpId);
        return PublicKeyCredentialRequestOptions.builder()
                .challenge(Bytes.random()) // Challenge should always be random
                .timeout(Duration.ofSeconds(60))
                .rpId(rpId)
                .allowCredentials( // Typically, this list would be populated based on the username/user handle
                        List.of(
                                PublicKeyCredentialDescriptor.builder()
                                        .id(Bytes.random()) // This would be a known credential ID for the user
                                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                                        .transports(Set.of(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID))
                                        .build()
                        )
                )
                .userVerification(UserVerificationRequirement.PREFERRED)
                .extensions(
                        new ImmutableAuthenticationExtensionsClientInputs(
                                // Example extension
                                // new CredProtectAuthenticationExtensionsClientInput(new CredProtect(ProtectionPolicy.USER_VERIFICATION_OPTIONAL, true))
                        )
                )
                .build();
    }
}
