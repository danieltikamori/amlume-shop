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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.KubernetesAuthenticationOptions;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;

/**
 * Usage examples at the end of code.
 */
@Configuration
public class MultipleVaultConfig {

    // --- Properties for Kubernetes Vault ---
    @Value("${vault.kubernetes.uri:http://vault.default.svc.cluster.local:8200}") // e.g., http://vault.default.svc.cluster.local:8200
    private String k8sVaultUri;

    @Value("${vault.kubernetes.role:reader}") // The Vault role configured for your K8s service account
    private String k8sVaultRole;

    @Value("${vault.kubernetes.auth-path:kubernetes}") // Default K8s auth path
    private String k8sAuthPath;

    // --- Properties for Local (Docker) container ---
    // Is defined at VaultProperties

    // --- Properties for (Docker) Container Vault ---
    // Injects the Vault server URI from the 'vault.uri' property.
    @Value("${vault.docker.uri}")
    private String containerVaultUri;

    // Injects the Vault authentication token from the 'vault.token' property.
    // Note: For production, consider more secure authentication methods like AppRole or Kubernetes Auth.
    @Value("${vault.token}")
    private String containerVaultToken;

    // --- Properties for HCP Vault (using Token Auth) ---
    @Value("${vault.hcp.uri}") // e.g., https://your-org.vault.cloud.hashicorp.com:8200
    private String hcpVaultUri;

    @Value("${vault.hcp.token}") // HCP Vault Token (consider AppRole for production)
    private String hcpVaultToken;

    // --- Bean Definition for Kubernetes Vault Template ---
    @Bean(name = "kubernetesVaultTemplate")
    @Profile("kubernetes")
//    @Profile("vault.kubernetes.enabled")
    public VaultTemplate kubernetesVaultTemplate(
            ClientAuthentication clientAuthentication, // Ensure this is the K8s auth bean
            VaultEndpoint vaultEndpoint // Ensure this points to the correct Vault instance
    ) {
        // The existing logic for creating the template using Kubernetes auth
        // likely involves creating KubernetesAuthenticationOptions and KubernetesAuthentication
        // which eventually leads to the error if run locally.
        // The @Profile annotation prevents this method from being called locally.

//        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(k8sVaultUri));

        // Configure Kubernetes Authentication
        KubernetesAuthenticationOptions options = KubernetesAuthenticationOptions.builder()
                .role(k8sVaultRole)
                .path(k8sAuthPath) // Specify the mount path if not default
                // .serviceAccountTokenFile(...) // Optional: specify token file if not default (if not set, it will use the default token file)
                .build(); // Causes the error if run locally

        // Assuming ClientAuthentication is correctly set up for k8s elsewhere
//        ClientAuthentication authentication = new KubernetesAuthentication(options, restOperations()); // restOperations() is provided by Spring Vault

        return new VaultTemplate(vaultEndpoint, clientAuthentication);
    }

    // --- Ensure you have other VaultTemplate beans for other profiles ---

    // For local development using Token Auth:
    @Bean
    @Profile("local") // Active for local profile
    public VaultTemplate localVaultTemplate(VaultProperties vaultProperties) {
        assert vaultProperties.getUri() != null;
        VaultEndpoint vaultEndpoint = VaultEndpoint.from(vaultProperties.getUri()); // Use local URI
        // Use TokenAuthentication for local development
        assert vaultProperties.getToken() != null;
        ClientAuthentication tokenAuth = new TokenAuthentication(vaultProperties.getToken());
        return new VaultTemplate(vaultEndpoint, tokenAuth);
    }


    // --- Bean Definition for (Docker) Container Vault Template (Token Auth) ---
    @Bean(name = "containerVaultTemplate")
    @ConditionalOnProperty(name = "vault.docker.enabled", havingValue = "true", matchIfMissing = false)
    public VaultTemplate containerVaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(containerVaultUri));

        // Configure Token Authentication
        ClientAuthentication authentication = new TokenAuthentication(containerVaultToken);

        return new VaultTemplate(endpoint, authentication);
    }

    // --- Bean Definition for HCP Vault Template (Token Auth) ---
    @Bean(name = "hcpVaultTemplate")
    @Profile("hcp") // Active for HCP profile
    public VaultTemplate hcpVaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(hcpVaultUri));

        // Configure Token Authentication
        ClientAuthentication authentication = new TokenAuthentication(hcpVaultToken);

        return new VaultTemplate(endpoint, authentication);
    }

    // --- Bean Definition for HCP Vault Template (AppRole Auth Example) ---
    /*
    @Value("${vault.hcp.approle.path:approle}")
    private String hcpAppRolePath;

    @Value("${vault.hcp.approle.role-id}")
    private String hcpAppRoleId;

    @Value("${vault.hcp.approle.secret-id}") // Fetch this securely!
    private String hcpAppRoleSecretId;

    @Bean(name = "hcpAppRoleVaultTemplate")
    public VaultTemplate hcpAppRoleVaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(hcpVaultUri));

        // Configure AppRole Authentication
        AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions.builder()
                .path(hcpAppRolePath)
                .roleId(AppRoleAuthenticationOptions.RoleId.provided(hcpAppRoleId))
                .secretId(AppRoleAuthenticationOptions.SecretId.provided(hcpAppRoleSecretId))
                .build();
        ClientAuthentication authentication = new AppRoleAuthentication(options, restOperations());

        return new VaultTemplate(endpoint, authentication);
    }
    */


    /**
     * Helper bean needed by some authentication methods like KubernetesAuthentication
     * and AppRoleAuthentication. Spring Vault provides this automatically when
     * extending AbstractVaultConfiguration, but we need it explicitly here.
     * You might already have a RestTemplate bean defined elsewhere.
     */
    @Bean
    @Qualifier("vaultRestOperations") // Give it a qualifier if you have other RestTemplates
    public org.springframework.web.client.RestOperations restOperations() {
        // Configure RestTemplate as needed (timeouts, etc.)
        return new org.springframework.web.client.RestTemplate();
    }
}

//        Usage examples
//Inject the Correct VaultTemplate using @QualifierIn your services where you need to interact with Vault, inject the specific VaultTemplate bean you require using the @Qualifier annotation with the bean name you defined:
//By defining distinct VaultTemplate beans with unique names and using @Qualifier for injection, you can cleanly manage multiple Vault connections within your application.
//
//package me.amlu.shop.amlume_shop.security.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//import org.springframework.vault.core.VaultTemplate;
//import org.springframework.vault.support.VaultResponse;
//
//@Service
//public class SomeInternalService {
//
//    private final VaultTemplate k8sVaultTemplate;
//
//    @Autowired
//    public SomeInternalService(@Qualifier("kubernetesVaultTemplate") VaultTemplate k8sVaultTemplate) {
//        this.k8sVaultTemplate = k8sVaultTemplate;
//    }
//
//    public String getInternalSecret(String path, String key) {
//        VaultResponse response = k8sVaultTemplate.read(path);
//        if (response != null && response.getData() != null) {
//            return (String) response.getData().get(key);
//        }
//        return null;
//    }
//}
//
//package me.amlu.shop.amlume_shop.security.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//import org.springframework.vault.core.VaultTemplate;
//import org.springframework.vault.support.VaultResponse;
//
//@Service
//public class SomeHcpService {
//
//    private final VaultTemplate hcpVaultTemplate;
//
//    // Injecting the HCP VaultTemplate (using Token Auth bean name from example)
//    @Autowired
//    public SomeHcpService(@Qualifier("hcpVaultTemplate") VaultTemplate hcpVaultTemplate) {
//        this.hcpVaultTemplate = hcpVaultTemplate;
//    }
//
//    // Or inject the AppRole one if you configured that
//    /*
//    @Autowired
//    public SomeHcpService(@Qualifier("hcpAppRoleVaultTemplate") VaultTemplate hcpVaultTemplate) {
//        this.hcpVaultTemplate = hcpVaultTemplate;
//    }
//    */
//
//    public String getHcpSecret(String path, String key) {
//        VaultResponse response = hcpVaultTemplate.read(path);
//        if (response != null && response.getData() != null) {
//            return (String) response.getData().get(key);
//        }
//        return null;
//    }
//}
