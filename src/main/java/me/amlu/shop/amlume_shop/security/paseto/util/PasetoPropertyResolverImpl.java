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

    //    @Autowired // Optional if using constructor injection with a single constructor
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

    @Override
    public String resolveAccessPublicKey() {
        return Objects.requireNonNull(pasetoProperties.getAccessPublicKey(), "Access Public Key not configured (paseto.access.public.public-key)");
    }

    @Override
    public String resolveAccessPrivateKey() {
        return Objects.requireNonNull(pasetoProperties.getAccessPrivateKey(), "Access Private Key not configured (paseto.access.public.private-key)");
    }

    @Override
    public String resolveAccessSecretKey() {
        return Objects.requireNonNull(pasetoProperties.getAccessSecretKey(), "Access Secret Key not configured (paseto.access.local.secret-key)");
    }

    @Override
    public String resolveRefreshSecretKey() {
        return Objects.requireNonNull(pasetoProperties.getRefreshSecretKey(), "Refresh Secret Key not configured (paseto.refresh.local.secret-key)");
    }

    @Override
    public String resolveRefreshPrivateKey() {
        return Objects.requireNonNull(pasetoProperties.getRefreshPrivateKey(), "Refresh Private Key not configured (paseto.refresh.public.private-key)");
    }

    @Override
    public String resolvePublicAccessKid() {
        return Objects.requireNonNull(pasetoProperties.getAccessPublicKid(), "Public Access KID not configured (paseto.access.public.kid)");
    }

    @Override
    public String resolvePublicRefreshKid() {
        return Objects.requireNonNull(pasetoProperties.getRefreshPublicKid(), "Public Refresh KID not configured (paseto.refresh.public.kid)");
    }

    @Override
    public String resolveLocalAccessKid() {
        return Objects.requireNonNull(pasetoProperties.getAccessLocalKid(), "Local Access KID not configured (paseto.access.local.kid)");
    }

    @Override
    public String resolveLocalRefreshKid() {
        return Objects.requireNonNull(pasetoProperties.getRefreshLocalKid(), "Local Refresh KID not configured (paseto.refresh.local.kid)");
    }
}