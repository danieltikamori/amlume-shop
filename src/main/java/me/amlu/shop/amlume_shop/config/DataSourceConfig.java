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

        @Value("${app.ssl.trust-store.path}")
        private String centralTruststorePath;

        @Value("${app.ssl.trust-store.password}")
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

            StringBuilder jdbcUrlBuilder = new StringBuilder(baseUrl);

            // Check if central truststore is configured
            boolean useSsl = StringUtils.hasText(centralTruststorePath) && StringUtils.hasText(centralTruststorePassword);

            if (useSsl) {
                log.info("Configuring MySQL DataSource with SSL using CENTRAL truststore: {}", centralTruststorePath);
                // Append SSL parameters for MySQL Connector/J
                // Ensure the base URL doesn't already contain '?'
                jdbcUrlBuilder.append(baseUrl.contains("?") ? "&" : "?")
                        .append("sslMode=VERIFY_CA") // Or VERIFY_IDENTITY if hostname verification is needed
                        .append("&requireSSL=true") // Explicitly require SSL
                        .append("&trustCertificateKeyStoreUrl=")
                        .append(URLEncoder.encode(centralTruststorePath, StandardCharsets.UTF_8)) // URL encode path
                        .append("&trustCertificateKeyStorePassword=")
                        .append(URLEncoder.encode(centralTruststorePassword, StandardCharsets.UTF_8)); // URL encode password
                // Add client cert params if needed: &clientCertificateKeyStoreUrl=...&clientCertificateKeyStorePassword=...
            } else {
                log.warn("Central truststore path or password not configured (app.ssl.trust-store.*). " +
                         "MySQL DataSource will be configured WITHOUT custom SSL truststore. " +
                         "Connection might fail or be insecure if the server requires trusted SSL.");
                // Optionally append sslMode=DISABLED or other non-verifying modes if needed
                // jdbcUrlBuilder.append(baseUrl.contains("?") ? "&" : "?").append("sslMode=DISABLED");
            }

            String finalJdbcUrl = jdbcUrlBuilder.toString();
            log.info("Final JDBC URL: {}", finalJdbcUrl); // Log the final URL (password will be encoded)

            return DataSourceBuilder.create()
                    .url(finalJdbcUrl)
                    .username(username)
                    .password(password) // Use the password resolved by Spring Boot
                    .driverClassName(driverClassName)
                    .build();
        }
    }
    