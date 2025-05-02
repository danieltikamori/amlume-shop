/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.stereotype.Component; // Or use @Configuration + @EnableConfigurationProperties
    import java.util.Set;
    import java.util.Collections;

    @Component // Make it a bean
    @ConfigurationProperties(prefix = "security.geo")
    public class GeoProperties {

//        private double suspiciousDistanceKm = 200.0; // Default value

        private Set<String> highRiskCountries = Collections.emptySet();


        // --- Getters and Setters ---
//        public double getSuspiciousDistanceKm() { return suspiciousDistanceKm; }
//        public void setSuspiciousDistanceKm(double suspiciousDistanceKm) { this.suspiciousDistanceKm = suspiciousDistanceKm; }

        public Set<String> getHighRiskCountries() { return highRiskCountries; }

        // No need for @NotEmpty here, as we are using Set and can return an empty set
        public void setHighRiskCountries(Set<String> highRiskCountries) { this.highRiskCountries = highRiskCountries; }
    }
    