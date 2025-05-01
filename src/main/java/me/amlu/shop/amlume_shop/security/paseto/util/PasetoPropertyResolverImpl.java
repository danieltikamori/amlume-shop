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
        String key = pasetoProperties.getPub() != null && pasetoProperties.getPub().getAccess() != null
                ? pasetoProperties.getPub().getAccess().getPublicKey()
                : null;
        return Objects.requireNonNull(key, "Access Public Key not configured (paseto.public.access.public-key)"); // Updated path
    }

    @Override
    public String resolveAccessPrivateKey() {
        String key = pasetoProperties.getPub() != null && pasetoProperties.getPub().getAccess() != null
                ? pasetoProperties.getPub().getAccess().getPrivateKey()
                : null;
        return Objects.requireNonNull(key, "Access Private Key not configured (paseto.public.access.private-key)"); // Updated path
    }

    @Override
    public String resolveAccessSecretKey() {
        String key = pasetoProperties.getLocal() != null && pasetoProperties.getLocal().getAccess() != null
                ? pasetoProperties.getLocal().getAccess().getSecretKey()
                : null;
        return Objects.requireNonNull(key, "Access Secret Key not configured (paseto.local.access.secret-key)"); // Updated path
    }

    @Override
    public String resolveRefreshSecretKey() {
        String key = pasetoProperties.getLocal() != null && pasetoProperties.getLocal().getRefresh() != null
                ? pasetoProperties.getLocal().getRefresh().getSecretKey()
                : null;
        return Objects.requireNonNull(key, "Refresh Secret Key not configured (paseto.local.refresh.secret-key)"); // Updated path
    }

    @Override
    public String resolveRefreshPrivateKey() {
        // Assuming public refresh keys if this method exists
        String key = pasetoProperties.getPub() != null && pasetoProperties.getPub().getRefresh() != null
                ? pasetoProperties.getPub().getRefresh().getPrivateKey()
                : null;
        return Objects.requireNonNull(key, "Refresh Private Key not configured (paseto.public.refresh.private-key)"); // Updated path
    }

    // --- KID Resolvers (Updated NOW) ---
    @Override
    public String resolvePublicAccessKid() {
        String kid = pasetoProperties.getPub() != null && pasetoProperties.getPub().getAccess() != null
                ? pasetoProperties.getPub().getAccess().getKid()
                : null;
        return Objects.requireNonNull(kid, "Public Access KID not configured (paseto.public.access.kid)"); // Updated path
    }

    @Override
    public String resolvePublicRefreshKid() {
        String kid = pasetoProperties.getPub() != null && pasetoProperties.getPub().getRefresh() != null
                ? pasetoProperties.getPub().getRefresh().getKid()
                : null;
        return Objects.requireNonNull(kid, "Public Refresh KID not configured (paseto.public.refresh.kid)"); // Updated path
    }

    @Override
    public String resolveLocalAccessKid() {
        String kid = pasetoProperties.getLocal() != null && pasetoProperties.getLocal().getAccess() != null
                ? pasetoProperties.getLocal().getAccess().getKid()
                : null;
        return Objects.requireNonNull(kid, "Local Access KID not configured (paseto.local.access.kid)"); // Updated path
    }

    @Override
    public String resolveLocalRefreshKid() {
        String kid = pasetoProperties.getLocal() != null && pasetoProperties.getLocal().getRefresh() != null
                ? pasetoProperties.getLocal().getRefresh().getKid()
                : null;
        return Objects.requireNonNull(kid, "Local Refresh KID not configured (paseto.local.refresh.kid)"); // Updated path
    }
}
