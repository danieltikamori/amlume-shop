/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto.util;

import me.amlu.shop.amlume_shop.config.properties.PasetoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * This class is responsible for resolving properties related to Paseto tokens.
 * It implements the PasetoPropertyResolver interface and uses the Environment object
 * or the injected PasetoProperties bean to retrieve the required properties.
 * <p>
 * Benefits of Property Resolver:
 * Provides a more flexible way to access the token configuration properties, as it allows you to resolve the properties at runtime using the PasetoPropertyResolver instance.
 * By using a property resolver, you decouple your components from the PasetoProperties instance and provide a more flexible way to access the paseto configuration properties.
 * <p>
 * Advantages:
 * <p>
 * Improved testability: You can easily mock the PasetoPropertyResolver instance and test the components that depend on it without relying on the actual PasetoProperties instance.
 * Decoupling: Your components are no longer tightly coupled to the PasetoProperties instance.
 * Flexibility: You can easily switch to a different configuration source or implementation without affecting your components.
 * Reusability: You can reuse the PasetoPropertyResolver instance across multiple components.
 * <p>
 * Note:
 * You can also use a caching mechanism to cache the resolved properties and improve performance.
 * <p>
 * *** IMPORTANT ***
 * This implementation mixes direct environment variable access (`System.getenv`) with access
 * via the injected `PasetoProperties` bean. This can lead to inconsistencies if properties
 * are defined in multiple places (e.g., `application.yml` AND environment variables).
 * It's generally recommended to rely *solely* on the `@ConfigurationProperties` bean (`PasetoProperties`)
 * for consistency and to leverage Spring Boot's property binding features (including environment variable overrides).
 * The direct `System.getenv` calls bypass this mechanism.
 * Consider refactoring to use only `pasetoProperties`.
 */
@Component
public class PasetoPropertyResolverImpl implements PasetoPropertyResolver {

    private final Environment environment; // Keep for properties not in PasetoProperties if needed
    private final PasetoProperties pasetoProperties; // Inject the properties bean

    @Autowired // Optional if using constructor injection with a single constructor
    public PasetoPropertyResolverImpl(Environment environment, PasetoProperties pasetoProperties) {
        this.environment = environment;
        this.pasetoProperties = pasetoProperties;
    }

    @Override
    public int resolveTokenNoFooterParts() {
        // Prefer using the injected properties bean
        return pasetoProperties.getTokenNoFooterParts();
        // Fallback to environment if needed, but less ideal:
        // return Integer.parseInt(Objects.requireNonNull(environment.getProperty("paseto.token-no-footer.parts")));
    }

    @Override
    public int resolveTokenWithFooterParts() {
        // Prefer using the injected properties bean
        return pasetoProperties.getTokenWithFooterParts();
        // Fallback to environment if needed, but less ideal:
        // return Integer.parseInt(Objects.requireNonNull(environment.getProperty("paseto.token-with-footer.parts")));
    }


    @Override
    public String resolveAccessPublicKey() {
        // Prefer using the injected properties bean
        return Objects.requireNonNull(pasetoProperties.getAccessPublicKey(), "Access Public Key not configured (paseto.access.public.public-key)");
        // Direct environment access (less recommended):
        // return Objects.requireNonNull(System.getenv("PASETO_ACCESS_PUBLIC_KEY"));
    }

    @Override
    public String resolveAccessSecretKey() {
        // Prefer using the injected properties bean
        return Objects.requireNonNull(pasetoProperties.getAccessSecretKey(), "Access Secret Key not configured (paseto.access.local.secret-key)");
        // Direct environment access (less recommended):
        // return Objects.requireNonNull(System.getenv("PASETO_ACCESS_SECRET_KEY"));
    }

    @Override
    public String resolveRefreshSecretKey() {
        // Prefer using the injected properties bean
        return Objects.requireNonNull(pasetoProperties.getRefreshSecretKey(), "Refresh Secret Key not configured (paseto.refresh.local.secret-key)");
        // Direct environment access (less recommended):
        // return Objects.requireNonNull(System.getenv("PASETO_REFRESH_SECRET_KEY"));
    }

    @Override
    public String resolvePublicAccessKid() {
        // Prefer using the injected properties bean
        // Note: PasetoProperties has accessPublicKid, not just a generic public access kid.
        return Objects.requireNonNull(pasetoProperties.getAccessPublicKid(), "Public Access KID not configured (paseto.access.public.kid)");
        // Direct environment access (less recommended, and uses a different env var name):
        // return System.getenv("PASETO_ACCESS_KID"); // Original code used PASETO_ACCESS_KID
    }

    @Override
    public String resolvePublicRefreshKid() {
        // Prefer using the injected properties bean
        return Objects.requireNonNull(pasetoProperties.getRefreshPublicKid(), "Public Refresh KID not configured (paseto.refresh.public.kid)");
        // Direct environment access (less recommended, and uses a different env var name):
        // return System.getenv("PASETO_REFRESH_KID"); // Original code used PASETO_REFRESH_KID
    }

    @Override
    public String resolveLocalAccessKid() {
        // Prefer using the injected properties bean
        return Objects.requireNonNull(pasetoProperties.getAccessLocalKid(), "Local Access KID not configured (paseto.access.local.kid)");
        // Direct environment access (less recommended):
        // return System.getenv("PASETO_ACCESS_LOCAL_KID");
    }

    @Override
    public String resolveLocalRefreshKid() {
        // Prefer using the injected properties bean
        return Objects.requireNonNull(pasetoProperties.getRefreshLocalKid(), "Local Refresh KID not configured (paseto.refresh.local.kid)");
        // Direct environment access (less recommended):
        // return System.getenv("PASETO_REFRESH_LOCAL_KID");
    }

}
