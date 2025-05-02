package me.amlu.shop.amlume_shop.config;/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

import me.amlu.shop.amlume_shop.config.properties.GeoProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class GeoConfig { // Or another appropriate @Configuration class

    private final GeoProperties geoProperties;

    // Inject the property value from YAML here
    @Value("${security.geo.high-risk-countries:}") // Default to empty string if not found
    private List<String> highRiskCountriesList; // Inject as List, Spring handles conversion from YAML list


    public GeoConfig(GeoProperties geoProperties) {
        this.geoProperties = geoProperties;
    }

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
        return geoProperties.getHighRiskCountries() != null ? new HashSet<>(geoProperties.getHighRiskCountries()) : Collections.emptySet();
    }
}
