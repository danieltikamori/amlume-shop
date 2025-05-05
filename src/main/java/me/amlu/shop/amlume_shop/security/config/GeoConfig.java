/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config;

// Removed: import me.amlu.shop.amlume_shop.security.config.properties.GeoProperties; // REMOVED
import me.amlu.shop.amlume_shop.security.config.properties.GeoSecurityProperties; // ADDED
// Removed: import org.springframework.beans.factory.annotation.Value; // REMOVED
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Collections;
import java.util.HashSet;
// Removed: import java.util.List; // REMOVED
import java.util.Set;

@Configuration
public class GeoConfig { // Or another appropriate @Configuration class

    // CHANGED: Use GeoSecurityProperties instead of GeoProperties
    private final GeoSecurityProperties geoSecurityProperties;

    // REMOVED: @Value injection is redundant as GeoSecurityProperties handles loading
    // @Value("${security.geo.high-risk-countries:}") // Default to empty string if not found
    // private List<String> highRiskCountriesList; // Inject as List, Spring handles conversion from YAML list


    // CHANGED: Constructor uses GeoSecurityProperties
    public GeoConfig(GeoSecurityProperties geoSecurityProperties) {
        this.geoSecurityProperties = geoSecurityProperties;
    }

    // REMOVED: Bean definition using @Value is no longer needed
//    @Bean("highRiskCountries") // Explicitly name the bean
//    public Set<String> highRiskCountriesSet() {
//        if (highRiskCountriesList == null || highRiskCountriesList.isEmpty()) {
//            return Collections.emptySet(); // Return immutable empty set if null/empty
//        }
//        // Convert the List to a Set
//        return new HashSet<>(highRiskCountriesList);
//    }

    @Bean("highRiskCountries") // Explicitly name the bean
    public Set<String> highRiskCountriesSet() {
        // CHANGED: Access properties via the injected GeoSecurityProperties bean
        return geoSecurityProperties.getHighRiskCountries() != null ? new HashSet<>(geoSecurityProperties.getHighRiskCountries()) : Collections.emptySet();
    }
}
