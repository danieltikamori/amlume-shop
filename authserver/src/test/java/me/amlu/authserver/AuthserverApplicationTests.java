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

    // // Load .env file using the system property passed by Maven
    // // If it does not work, use hardcoded values
    // private static final Dotenv dotenv = Dotenv.configure()
    //         .directory(System.getProperty("project.root.basedir")) // <-- Use system property
    //         .ignoreIfMissing() // Keep this, good practice
    //         .load();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        // --- TEMPORARILY HARDCODE VALUES ---
        // String dbUser = dotenv.get("AUTH_DB_USER"); // Still using hardcoded for now
        // String dbPassword = dotenv.get("AUTH_DB_PASSWORD"); // Still using hardcoded for now
        String dbUser = "auth_server_user"; // Use the actual username from .env
        String dbPassword = "C0961F7D8F83574BEA49E9B412BBA60704CD720C"; // Use the actual password from .env
        // ------------------------------------


        // --- CORRECT THE JDBC URL STRING ---
        String jdbcUrl = "jdbc:mysql://localhost:3406/amlume_db?sslMode=DISABLED&allowPublicKeyRetrieval=true"; // REMOVED "url: " and "\n"
        // -----------------------------------

        // Debugging - Check if the password is now loaded
        System.out.println("Test Project Root Dir: " + System.getProperty("project.root.basedir"));
        System.out.println("Test JDBC URL: " + jdbcUrl); // Should print the correct URL now

        // Add properties to registry
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> dbUser);
        registry.add("spring.datasource.password", () -> dbPassword);
        // registry.add("app.ssl.trust-store.password", () -> truststorePassword); // Still commented out
        // registry.add("app.ssl.trust-store.path", () -> "classpath:config/central-truststore.jks"); // Still commented out
    }

    @Test
    void contextLoads() {
        // Test context loading
    }
}
    