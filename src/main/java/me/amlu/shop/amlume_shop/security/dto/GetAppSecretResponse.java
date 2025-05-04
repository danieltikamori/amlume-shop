    /*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

    package me.amlu.shop.amlume_shop.security.dto;

    import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
    import com.fasterxml.jackson.annotation.JsonProperty;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GetAppSecretResponse(
            Secret secret // Matches the top-level 'secret' object in the API response
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Secret(
                String name,
                String type,
                @JsonProperty("latest_version")
                Integer latestVersion,
                @JsonProperty("created_at")
                String createdAt,
                // Assuming you need the value from static_version
                // Make sure this matches the actual structure returned by GetAppSecret
                @JsonProperty("static_version")
                SecretStaticVersion staticVersion
                // Add other fields from the API response as needed
        ) {}

        // Reusing SecretStaticVersion - ensure it has 'value' and 'version'
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SecretStaticVersion(
                String value,
                Integer version
                // Add other fields if needed from GetAppSecret's static_version response
        ) {}

        // Add other nested records like CreatedBy if necessary
    }