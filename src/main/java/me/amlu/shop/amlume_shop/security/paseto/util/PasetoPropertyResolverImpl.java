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

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * This class is responsible for resolving properties related to Paseto tokens.
 * It implements the PasetoPropertyResolver interface and uses the Environment object
 * to retrieve the required properties.
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
 */
@Component
public class PasetoPropertyResolverImpl implements PasetoPropertyResolver {

    private Environment environment;

    @Override
    public int resolveTokenNoFooterParts() {
        return Integer.parseInt(Objects.requireNonNull(environment.getProperty("paseto.token-no-footer.parts")));
    }

    @Override
    public int resolveTokenWithFooterParts() {
        return Integer.parseInt(Objects.requireNonNull(environment.getProperty("paseto.token-with-footer.parts")));
    }


    @Override
    public String resolveAccessPublicKey() {
        return Objects.requireNonNull(System.getenv("PASETO_ACCESS_PUBLIC_KEY"));
    }

    @Override
    public String resolveAccessSecretKey() {
        return Objects.requireNonNull(System.getenv("PASETO_ACCESS_SECRET_KEY"));
    }

    @Override
    public String resolveRefreshSecretKey() {
        return Objects.requireNonNull(System.getenv("PASETO_REFRESH_SECRET_KEY"));
    }

    @Override
    public String resolvePublicAccessKid() {
        return System.getenv("PASETO_ACCESS_KID");
    }

    @Override
    public String resolvePublicRefreshKid() {
        return System.getenv("PASETO_REFRESH_KID");
    }

    @Override
    public String resolveLocalAccessKid() {
        return System.getenv("PASETO_ACCESS_LOCAL_KID");
    }

    @Override
    public String resolveLocalRefreshKid() {
        return System.getenv("PASETO_REFRESH_LOCAL_KID");
    }

}
