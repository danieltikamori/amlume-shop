/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto;

import me.amlu.shop.amlume_shop.security.config.properties.TokenProperties;
import org.springframework.stereotype.Service;

/**
 * Benefits of using a configuration service:
 * By using a configuration service, you decouple your components from the TokenProperties instance and provide a more flexible way to access the token configuration properties.
 * <p>
 * Advantages:
 * Decoupling: Your components are no longer tightly coupled to the TokenProperties instance.
 * Flexibility: You can easily switch to a different configuration source or implementation without affecting your components.
 * Reusability: You can reuse the TokenConfigurationService instance across multiple components.
 * <p>
 * Note:
 * You can also use a caching mechanism to cache the token configuration properties and improve performance.
 */


@Service
public class TokenConfigurationServiceImpl implements TokenConfigurationService {

    private TokenProperties tokenProperties;

    public TokenConfigurationServiceImpl(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }


    @Override
    public long getAccessTokenValidity() {
        return tokenProperties.getAccessValidity().toMillis();
    }

    @Override
    public long getRefreshTokenValidity() {
        return tokenProperties.getRefreshValidity().toMillis();
    }

    @Override
    public double getClaimsValidationRateLimitPermitsPerSecond() {
        return tokenProperties.getClaimsValidationRateLimitPermitsPerSecond();
    }

    @Override
    public double getValidationRateLimitPermitsPerSecond() {
        return tokenProperties.getValidationRateLimitPermitsPerSecond();
    }

   @Override
   public int getValidationRateLimitMaxAttempts() {
       return tokenProperties.getValidationRateLimitMaxAttempts();
   }

   @Override
   public int getValidationRateLimitWindowSeconds() {
       return tokenProperties.getValidationRateLimitWindowSeconds();
   }


@Override
public int getValidationRateLimitWindowSizeInSeconds() {
	// TODO Auto-generated method stub
	return 0;
}
}
