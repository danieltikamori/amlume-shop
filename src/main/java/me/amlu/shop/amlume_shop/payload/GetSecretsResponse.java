/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 * ... (rest of copyright notice) ...
 */

package me.amlu.shop.amlume_shop.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty; // Import JsonProperty
import java.util.List;
// Removed unused import: import java.util.Map;

/**
 * Represents the response structure from the HCP Secrets API endpoint (.../secrets:open),
 * including secrets and pagination information.
 *
 * @param secrets    List of secret details retrieved.
 * @param pagination Pagination details from the API response.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore fields not mapped at the top level
public record GetSecretsResponse(
        List<SecretDetail> secrets, // The "secrets" field is a List
        Pagination pagination       // Field for the pagination object
) {

    /**
     * Represents a single secret entry within the response array.
     *
     * @param name          The name of the secret (e.g., "MFA_ENCRYPTION_PASSWORD").
     * @param staticVersion The object containing the actual secret value and its version details.
     *                      Mapped from the JSON key "static_version".
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SecretDetail(
            String name,
            @JsonProperty("static_version") // Map the JSON key "static_version"
            SecretVersion staticVersion
    ) {}

    /**
     * Represents the version details of a secret, including its value.
     * Mapped from the JSON object under the "static_version" key.
     *
     * @param version The version number of the secret (integer).
     * @param value   The actual secret value (string).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SecretVersion(
            int version, // Correct type: int matches the JSON number
            String value
    ) {}

    /**
     * Represents the pagination details returned by the HCP Secrets API.
     *
     * @param nextPageToken     Token to use for fetching the next page of results.
     * @param previousPageToken Token to use for fetching the previous page of results.
     */
    @JsonIgnoreProperties(ignoreUnknown = true) // ADDED: Ignore unknown fields in pagination
    public record Pagination(
            @JsonProperty("next_page_token") // Map JSON key to Java field
            String nextPageToken,

            @JsonProperty("previous_page_token") // Map JSON key to Java field
            String previousPageToken
    ) {}
}