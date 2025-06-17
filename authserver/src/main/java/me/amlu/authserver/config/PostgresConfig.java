/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration for PostgreSQL with LTREE extension support.
 */
@Configuration
public class PostgresConfig {

    private final DataSource dataSource;
    private final Environment env;

    @Autowired
    public PostgresConfig(DataSource dataSource, Environment env) {
        this.dataSource = dataSource;
        this.env = env;
    }

    /**
     * Initialize PostgreSQL with LTREE extension if needed.
     * This is required for hierarchical role queries.
     */
    @Bean
    public JdbcTemplate jdbcTemplate() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Only run in non-test environments
        if (!isTestEnvironment()) {
            // Create LTREE extension if it doesn't exist
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS ltree");
        }

        return jdbcTemplate;
    }

    private boolean isTestEnvironment() {
        String[] activeProfiles = env.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.contains("test")) {
                return true;
            }
        }
        return false;
    }
}
