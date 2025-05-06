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

    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
    import org.springframework.boot.jdbc.DataSourceBuilder;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.util.StringUtils;

    import javax.sql.DataSource;
    import java.net.URLEncoder;
    import java.nio.charset.StandardCharsets;

    @Configuration
    public class DataSourceConfig {

        private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

        // Use field injection for optional properties or ensure they are always set
        @Value("${app.ssl.trust-store.path:file:./config/central-truststore.jks}") // Default to null if not set
        private String centralTruststorePath;

        @Value("${app.ssl.trust-store.password:#{null}}")
        private String centralTruststorePassword;

        // Inject Spring Boot's resolved DataSourceProperties
        private final DataSourceProperties dataSourceProperties;

        public DataSourceConfig(DataSourceProperties dataSourceProperties) {
            this.dataSourceProperties = dataSourceProperties;
        }

        @Bean
        public DataSource dataSource() {
            String baseUrl = dataSourceProperties.getUrl();
            String username = dataSourceProperties.getUsername();
            String password = dataSourceProperties.getPassword(); // Already resolved by Spring Boot
            String driverClassName = dataSourceProperties.getDriverClassName();

            if (baseUrl == null) {
                throw new IllegalStateException("spring.datasource.url is not configured");
            }

            StringBuilder jdbcUrlBuilder = new StringBuilder(baseUrl);

            // Check if central truststore is configured AND SSL is not explicitly disabled in the base URL
            boolean truststoreConfigured = StringUtils.hasText(centralTruststorePath) && StringUtils.hasText(centralTruststorePassword);
            boolean sslExplicitlyDisabled = baseUrl.contains("sslMode=DISABLED"); // Check for explicit disable

            // Determine if SSL should be actively configured by this class
            boolean configureSsl = truststoreConfigured && !sslExplicitlyDisabled;

            if (configureSsl) {
                log.info("Configuring MySQL DataSource with SSL using CENTRAL truststore: {}", centralTruststorePath);
                // Append SSL parameters ONLY if not already present or conflicting
                // A more robust check might parse existing parameters, but this handles the common case.
                if (!baseUrl.contains("sslMode=") && !baseUrl.contains("requireSSL=")) {
                    try {
                        jdbcUrlBuilder.append(baseUrl.contains("?") ? "&" : "?")
                                .append("sslMode=VERIFY_CA") // Or VERIFY_IDENTITY
                                .append("&requireSSL=true")
                                .append("&trustCertificateKeyStoreUrl=")
                                .append(URLEncoder.encode(centralTruststorePath, StandardCharsets.UTF_8))
                                .append("&trustCertificateKeyStoreType=JKS") // Or PKCS12
                                .append("&trustCertificateKeyStorePassword=")
                                .append(URLEncoder.encode(centralTruststorePassword, StandardCharsets.UTF_8));
                        // Add client cert params if needed
                    } catch (Exception e) { // Catch potential encoding errors
                        log.error("Error encoding SSL parameters for JDBC URL", e);
                        // Decide how to handle: throw exception, fallback, etc.
                        // For now, we'll proceed without the SSL params if encoding fails
                        // Reset the builder to the base URL to avoid partial SSL config
                        jdbcUrlBuilder = new StringBuilder(baseUrl);
                        log.warn("Proceeding without custom SSL configuration due to encoding error.");
                    }
                } else {
                    log.warn("Base JDBC URL already contains SSL parameters. Skipping automatic SSL configuration from DataSourceConfig to avoid conflicts. Base URL: {}", baseUrl);
                }
            } else if (sslExplicitlyDisabled) {
                log.info("SSL explicitly disabled in base JDBC URL (sslMode=DISABLED). Skipping custom SSL configuration.");
            } else if (!truststoreConfigured) {
                log.warn("Central truststore path or password not configured (app.ssl.trust-store.*). " +
                         "MySQL DataSource will be configured WITHOUT custom SSL truststore from DataSourceConfig. " +
                         "Connection might fail or be insecure if the server requires trusted SSL and base URL doesn't configure it.");
                // Base URL might still have SSL settings like sslMode=REQUIRED, which would be used.
            }

            String finalJdbcUrl = jdbcUrlBuilder.toString();
            // Avoid logging the full URL with password in production logs if possible.
            // Consider logging only parts or using a masked version for debugging
            log.info("Final JDBC URL prepared: {}", finalJdbcUrl.replace(centralTruststorePassword, "****")); // Basic masking

            return DataSourceBuilder.create()
                    .url(finalJdbcUrl)
                    .username(username)
                    .password(password) // Use the password resolved by Spring Boot
                    .driverClassName(driverClassName)
                    .build();
        }
    }
    