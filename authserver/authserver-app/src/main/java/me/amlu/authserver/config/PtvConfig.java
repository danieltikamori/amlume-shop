/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
class PtvConfig {
    private static final Logger log = LoggerFactory.getLogger(PtvConfig.class);

    @Bean
    @Primary
    public PolymorphicTypeValidator polymorphicTypeValidator() {
        log.info("Creating primary PolymorphicTypeValidator bean");
        return createBasePolymorphicTypeValidator().build();
    }

    @Bean
    @Qualifier("customSessionPolymorphicTypeValidator")
    public PolymorphicTypeValidator sessionPolymorphicTypeValidator() {
        log.info("Creating session-specific PolymorphicTypeValidator bean");
        return createBasePolymorphicTypeValidator()
                // Additional session-specific types
                .allowIfSubType(org.springframework.security.web.savedrequest.SavedRequest.class)
                .allowIfSubType(org.springframework.security.web.savedrequest.DefaultSavedRequest.class)
                .allowIfSubType(org.springframework.security.web.savedrequest.SavedCookie.class)
                .allowIfSubType(org.springframework.security.web.csrf.DefaultCsrfToken.class)
                .allowIfSubType(org.springframework.security.web.authentication.WebAuthenticationDetails.class)
                .allowIfSubType(org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken.class)
                .allowIfSubType(org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority.class)
                .allowIfSubType(org.springframework.security.oauth2.core.OAuth2AccessToken.class)
                .allowIfSubType(org.springframework.security.oauth2.core.OAuth2RefreshToken.class)
                .allowIfSubType(org.springframework.security.oauth2.core.oidc.OidcIdToken.class)
                .allowIfSubType(org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser.class)
                .allowIfSubType(org.springframework.security.oauth2.core.user.DefaultOAuth2User.class)
                .allowIfBaseType("org.springframework.security.oauth2.client.")
                .build();
    }

    @Bean
    @Qualifier("webAuthnPolymorphicTypeValidator")
    public PolymorphicTypeValidator webAuthnPolymorphicTypeValidator() {
        log.info("Creating WebAuthn-specific PolymorphicTypeValidator bean");
        return createBasePolymorphicTypeValidator()
                // Use base package patterns for WebAuthn types
                .allowIfBaseType("com.webauthn4j.")
                .allowIfBaseType("com.webauthn4j.springframework.security.")
                .allowIfBaseType("org.springframework.security.web.webauthn.")
                .allowIfSubType(org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions.class)
                .allowIfSubType(org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions.class)
                .build();
    }

    /**
     * Creates a base PolymorphicTypeValidator.Builder with common type allowances
     * that can be extended for specific use cases.
     */
    private BasicPolymorphicTypeValidator.Builder createBasePolymorphicTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                // Standard Java types
                .allowIfBaseType("java.util.")
                .allowIfBaseType("java.time.")
                .allowIfBaseType("java.lang.")

                // Collections and Maps
                .allowIfSubType(java.util.ArrayList.class)
                .allowIfSubType(java.util.LinkedList.class)
                .allowIfSubType(java.util.HashSet.class)
                .allowIfSubType(java.util.LinkedHashSet.class)
                .allowIfSubType(java.util.TreeSet.class)
                .allowIfSubType(java.util.HashMap.class)
                .allowIfSubType(java.util.LinkedHashMap.class)
                .allowIfSubType(java.util.TreeMap.class)
                .allowIfSubType(java.util.concurrent.ConcurrentHashMap.class)

                // Spring Security framework types
                .allowIfBaseType("org.springframework.security.")
                .allowIfSubType(org.springframework.security.core.context.SecurityContextImpl.class)
                .allowIfSubType(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class)
                .allowIfSubType(org.springframework.security.core.authority.SimpleGrantedAuthority.class)
                .allowIfSubType(org.springframework.security.authentication.AnonymousAuthenticationToken.class)
                .allowIfSubType(org.springframework.security.authentication.RememberMeAuthenticationToken.class)

                // The application's types
                .allowIfSubType(me.amlu.authserver.user.model.User.class)
                .allowIfBaseType("me.amlu.authserver.user.model.vo.")
                .allowIfBaseType("me.amlu.authserver.oauth2.model.")
                .allowIfBaseType("me.amlu.authserver.passkey.model.")
                .allowIfBaseType("me.amlu.authserver.");
    }
}
