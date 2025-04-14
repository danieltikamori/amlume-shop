/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

import java.net.URI;

/**
 * Configures the connection to HashiCorp Vault using Spring Vault.
 * This class extends AbstractVaultConfiguration to provide the necessary beans
 * for Vault endpoint and client authentication.
 * <p>
 * It reads the Vault URI and authentication token from application properties
 * (or environment variables).
 */
@Configuration
public class VaultConfig extends AbstractVaultConfiguration {

    // Injects the Vault server URI from the 'vault.uri' property.
    @Value("${vault.uri}")
    private String vaultUri;

    // Injects the Vault authentication token from the 'vault.token' property.
    // Note: For production, consider more secure authentication methods like AppRole or Kubernetes Auth.
    @Value("${vault.token}")
    private String vaultToken;

    /**
     * Defines the Vault server endpoint.
     * Reads the URI from the injected vaultUri property.
     *
     * @return VaultEndpoint representing the Vault server location.
     */
    @NotNull
    @Override
    public VaultEndpoint vaultEndpoint() {
        // Creates the VaultEndpoint from the configured URI string.
        return VaultEndpoint.from(URI.create(vaultUri));
    }

    /**
     * Defines the client authentication method for connecting to Vault.
     * Uses TokenAuthentication with the token injected from the vaultToken property.
     *
     * @return ClientAuthentication instance configured for token authentication.
     */
    @NotNull
    @Override
    public ClientAuthentication clientAuthentication() {
        // Creates a TokenAuthentication object using the configured Vault token.
        return new TokenAuthentication(vaultToken);
    }
}