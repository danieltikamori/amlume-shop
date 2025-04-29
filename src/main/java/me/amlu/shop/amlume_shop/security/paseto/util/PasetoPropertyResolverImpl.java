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
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * This class is responsible for resolving properties related to Paseto tokens.
 * It implements the PasetoPropertyResolver interface and uses the injected PasetoProperties bean
 * to retrieve the required properties.
 * <p>
 * Benefits of Property Resolver:
 * Provides a flexible way to access token configuration properties.
 * Decouples components from the specific PasetoProperties class structure.
 * Improves testability by allowing mocking of the resolver.
 * <p>
 * *** NOTE ***
 * This implementation now relies solely on the PasetoProperties bean, which is the recommended approach
 * for consistency with Spring Boot's property binding mechanisms.
 */
@Component
public class PasetoPropertyResolverImpl implements PasetoPropertyResolver {

    private final PasetoProperties pasetoProperties; // Inject the properties bean

    public PasetoPropertyResolverImpl(PasetoProperties pasetoProperties) {
        this.pasetoProperties = pasetoProperties;
    }

    @Override
    public int resolveTokenNoFooterParts() {
        return pasetoProperties.getTokenNoFooterParts();
    }

    @Override
    public int resolveTokenWithFooterParts() {
        return pasetoProperties.getTokenWithFooterParts();
    }

    // --- Key Material Resolvers (Updated previously) ---
    @Override
    public String resolveAccessPublicKey() {
        String key = pasetoProperties.getAccess() != null && pasetoProperties.getAccess().getPub() != null
                ? pasetoProperties.getAccess().getPub().getPublicKey()
                : null;
        return Objects.requireNonNull(key, "Access Public Key not configured (paseto.access.public.public-key)");
    }

    @Override
    public String resolveAccessPrivateKey() {
        String key = pasetoProperties.getAccess() != null && pasetoProperties.getAccess().getPub() != null
                ? pasetoProperties.getAccess().getPub().getPrivateKey()
                : null;
        return Objects.requireNonNull(key, "Access Private Key not configured (paseto.access.public.private-key)");
    }

    @Override
    public String resolveAccessSecretKey() {
        String key = pasetoProperties.getAccess() != null && pasetoProperties.getAccess().getLocal() != null
                ? pasetoProperties.getAccess().getLocal().getSecretKey()
                : null;
        return Objects.requireNonNull(key, "Access Secret Key not configured (paseto.access.local.secret-key)");
    }

    @Override
    public String resolveRefreshSecretKey() {
        String key = pasetoProperties.getRefresh() != null && pasetoProperties.getRefresh().getLocal() != null
                ? pasetoProperties.getRefresh().getLocal().getSecretKey()
                : null;
        return Objects.requireNonNull(key, "Refresh Secret Key not configured (paseto.refresh.local.secret-key)");
    }

    @Override
    public String resolveRefreshPrivateKey() {
        String key = pasetoProperties.getRefresh() != null && pasetoProperties.getRefresh().getPub() != null
                ? pasetoProperties.getRefresh().getPub().getPrivateKey()
                : null;
        return Objects.requireNonNull(key, "Refresh Private Key not configured (paseto.refresh.public.private-key)");
    }

    // --- KID Resolvers (Updated NOW) ---
    @Override
    public String resolvePublicAccessKid() {
        // Updated access path
        String kid = pasetoProperties.getAccess() != null && pasetoProperties.getAccess().getPub() != null
                ? pasetoProperties.getAccess().getPub().getKid()
                : null;
        return Objects.requireNonNull(kid, "Public Access KID not configured (paseto.access.public.kid)");
    }

    @Override
    public String resolvePublicRefreshKid() {
        // Updated access path
        String kid = pasetoProperties.getRefresh() != null && pasetoProperties.getRefresh().getPub() != null
                ? pasetoProperties.getRefresh().getPub().getKid()
                : null;
        return Objects.requireNonNull(kid, "Public Refresh KID not configured (paseto.refresh.public.kid)");
    }

    @Override
    public String resolveLocalAccessKid() {
        // Updated access path
        String kid = pasetoProperties.getAccess() != null && pasetoProperties.getAccess().getLocal() != null
                ? pasetoProperties.getAccess().getLocal().getKid()
                : null;
        return Objects.requireNonNull(kid, "Local Access KID not configured (paseto.access.local.kid)");
    }

    @Override
    public String resolveLocalRefreshKid() {
        // Updated access path
        String kid = pasetoProperties.getRefresh() != null && pasetoProperties.getRefresh().getLocal() != null
                ? pasetoProperties.getRefresh().getLocal().getKid()
                : null;
        return Objects.requireNonNull(kid, "Local Refresh KID not configured (paseto.refresh.local.kid)");
    }
}