/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.health;

import me.amlu.authserver.resilience.service.ResilientDatabaseService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for monitoring database connectivity.
 * <p>
 * This component reports the health status of the database connection.
 * It performs a connectivity test and collects metadata about the database.
 * </p>
 * <p>
 * The health report includes:
 * <ul>
 *   <li>Database product name</li>
 *   <li>Database version</li>
 *   <li>JDBC driver information</li>
 *   <li>Connection validation status</li>
 * </ul>
 * </p>
 * <p>
 * If the database is unavailable, it reports DOWN status with error details.
 * </p>
 */

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final ResilientDatabaseService databaseService;

    public DatabaseHealthIndicator(DataSource dataSource, ResilientDatabaseService databaseService) {
        this.dataSource = dataSource;
        this.databaseService = databaseService;
    }

    @Override
    public Health health() {
        if (!databaseService.isHealthy()) {
            return Health.down().withDetail("message", "Database connectivity test failed").build();
        }

        Map<String, Object> details = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            details.put("database", metaData.getDatabaseProductName());
            details.put("version", metaData.getDatabaseProductVersion());
            details.put("driver", metaData.getDriverName() + " " + metaData.getDriverVersion());
            details.put("validationQuery", "SELECT 1");

            // Check if connection is valid with a short timeout
            boolean valid = connection.isValid(2);
            if (!valid) {
                return Health.down().withDetails(details).build();
            }

            return Health.up().withDetails(details).build();
        } catch (SQLException e) {
            details.put("error", e.getMessage());
            details.put("errorCode", e.getErrorCode());
            details.put("sqlState", e.getSQLState());
            return Health.down().withDetails(details).build();
        }
    }
}
