/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.security;

import me.amlu.shop.amlume_shop.config.properties.MfaProperties;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.StringUtils;

/**
 * Configuration for encryption, specifically for the TextEncryptor used for MFA secrets.
 * <p>
 * This configuration attempts to retrieve the encryption password and salt from HashiCorp Vault
 * via Spring Cloud Vault.
 * If Vault is unavailable at startup (and fail-fast is false) or if
 * the specific secrets are not found in Vault, it falls back to using environment variables
 * defined by the properties `mfa.encryption.password` and `mfa.encryption.salt`.
 * <p>
 * Prerequisites:
 * 1. `spring-cloud-starter-vault-config` dependency must be included.
 * 2. Vault connection details (`spring.cloud.vault.uri`, `spring.cloud.vault.token`, etc.)
 * must be configured in `application.yml` or `bootstrap.yml`.
 * 3. `spring.cloud.vault.fail-fast` should be set to `false` in configuration to allow fallback.
 * 4. `spring.config.import` should include `optional:vault://` for graceful startup if Vault is unreachable.
 * 5. The secrets `mfa.encryption.password` and `mfa.encryption.salt` should exist in Vault
 * under the path configured for Spring Cloud Vault (e.g., `secret/amlume-shop/mfa`).
 * 6. Environment variables `MFA_ENCRYPTION_PASSWORD` and `MFA_ENCRYPTION_SALT` must be set
 * in the deployment environment to serve as the ultimate fallback.
 */
@Configuration
public class EncryptionConfig {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EncryptionConfig.class);
    // Set in MfaProperties class to allow for easier testing and management. @Value usage is discouraged, deprecated.
    // if it successfully fetches them. Defaults to null if not found anywhere.
//    @Value("${mfa.encryption.password:#{null}}")
//    private String mfaEncryptionPassword;

//    @Value("${mfa.encryption.salt:#{null}}")
//    private String mfaSalt;

    private final MfaProperties mfaProperties;
    private final Environment environment;

    // Inject Environment to check if Vault properties are active
    public EncryptionConfig(MfaProperties mfaProperties, Environment environment) {
        this.mfaProperties = mfaProperties;
        this.environment = environment;
    }

    @Bean
    public TextEncryptor textEncryptor() {
        // Check if the properties were successfully injected from any source
        if (!StringUtils.hasText(mfaProperties.getMfaEncryptionPassword()) || !StringUtils.hasText(mfaProperties.getMfaEncryptionSalt())) {
            log.error("CRITICAL: MFA encryption password or salt is missing! " +
                    "Ensure secrets are set in Vault ('mfa.encryption.password', 'mfa.encryption.salt') " +
                    "or as fallback environment variables ('MFA_ENCRYPTION_PASSWORD', 'MFA_ENCRYPTION_SALT').");
            // Throwing an exception prevents the application from starting with insecure defaults.
            throw new IllegalStateException("MFA encryption password or salt could not be resolved.");
        }

        // Log which source is likely being used (heuristic based on Vault property source presence)
        // Note: This check confirms Vault integration is active, not necessarily that *these specific*
        // properties came from Vault (they could still be from the fallback env vars if missing in Vault).
        boolean isVaultConfigured = false; // Default to false
        // Cast Environment to ConfigurableEnvironment to access getPropertySources()
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            isVaultConfigured = configurableEnvironment.getPropertySources().stream()
                    .anyMatch(source -> source.getName().startsWith("vault"));
        } else {
            log.warn("Environment is not an instance of ConfigurableEnvironment. Cannot check for Vault property sources.");
            // Decide how to handle this - maybe assume Vault isn't configured?
        }


        if (isVaultConfigured) {
            log.info("Vault property source detected. Attempting to use MFA encryption secrets potentially sourced from Vault.");
            // You could add more specific checks here if needed, e.g., checking the PropertySource of the resolved value,
            // but that adds complexity.
        } else {
            log.warn("Vault property source not detected. Falling back to environment variables for MFA encryption secrets. " +
                    "Ensure MFA_ENCRYPTION_PASSWORD and MFA_ENCRYPTION_SALT are securely set.");
        }

        // Log confirmation without exposing secrets
        log.info("Configuring TextEncryptor for MFA secrets. Password and Salt have been provided (lengths: {}, {}).",
                mfaProperties.getMfaEncryptionPassword().length(), mfaProperties.getMfaEncryptionSalt().length());

        // Create the TextEncryptor using the resolved password and salt
        return Encryptors.text(mfaProperties.getMfaEncryptionPassword(), mfaProperties.getMfaEncryptionSalt());
    }
}