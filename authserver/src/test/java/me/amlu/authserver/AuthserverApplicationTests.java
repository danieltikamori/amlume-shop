/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class AuthserverApplicationTests {

    // Load .env file using the system property passed by Maven
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(System.getProperty("project.root.basedir")) // <-- Use system property
            .ignoreIfMissing() // Keep this, good practice
            .load();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String dbUser = dotenv.get("AUTH_DB_USER");
        String dbPassword = dotenv.get("AUTH_DB_PASSWORD");
        String truststorePassword = dotenv.get("APP_CENTRAL_TRUSTSTORE_PASSWORD"); // Should now load correctly

        // Construct the full JDBC URL with the correct password embedded
        String jdbcUrl = String.format(
                "jdbc:mysql://localhost:3306/amlume_db?sslMode=VERIFY_CA&trustCertificateKeyStoreUrl=file:./config/central-truststore.jks&trustCertificateKeyStoreType=JKS&trustCertificateKeyStorePassword=%s",
                truststorePassword
        );

        // Debugging - Check if the password is now loaded
        System.out.println("Test Project Root Dir: " + System.getProperty("project.root.basedir"));
        System.out.println("Test JDBC URL: " + jdbcUrl);
        System.out.println("Test Truststore Password: " + truststorePassword); // <-- Should NOT be null now

        // Add properties to registry
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> dbUser);
        registry.add("spring.datasource.password", () -> dbPassword);
        registry.add("app.ssl.trust-store.password", () -> truststorePassword);
        registry.add("app.ssl.trust-store.path", () -> "classpath:config/central-truststore.jks");
    }

    @Test
    void contextLoads() {
        // Test context loading
    }
}
    