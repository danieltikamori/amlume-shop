/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the response from the HCP Secrets API endpoint for opening secrets.
 * Maps to the 'secrets_20231128OpenAppSecretsResponse' definition in the Swagger spec.
 * Includes nested records for SecretDetail, SecretStaticVersion, and PaginationInfo
 * based on the structure used in HCPSecretsService.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore fields not explicitly defined
public record GetSecretsResponse(
        List<SecretDetail> secrets,
        PaginationInfo pagination
) {

    /**
     * Represents the static version details of a secret.
     * Maps to relevant parts of 'secrets_20231128OpenSecretStaticVersion'.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SecretStaticVersion(
            String value
            // Other fields like version, createdAt, createdById exist in Swagger but are ignored here
    ) {}

    /**
     * Represents the details of a single secret.
     * Maps to relevant parts of 'secrets_20231128OpenSecret'.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SecretDetail(
            String name,
            @JsonProperty("static_version") // Map snake_case JSON field
            SecretStaticVersion staticVersion
            // Other fields like type, provider, latest_version, etc., exist in Swagger but are ignored here
    ) {}

    /**
     * Represents pagination information returned by the API.
     * Maps to relevant parts of 'commonPaginationResponse'.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaginationInfo(
            @JsonProperty("next_page_token") // Map snake_case JSON field
            String nextPageToken
            // previous_page_token exists in Swagger but is ignored here
    ) {}
}