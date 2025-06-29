/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.resilience.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * A resilient database service with circuit breaker, bulkhead, and retry patterns.
 * <p>
 * This service provides low-level database operations with resilience patterns
 * to handle database failures gracefully. It includes:
 * <ul>
 *   <li>Circuit breakers to prevent cascading failures</li>
 *   <li>Bulkheads to isolate database operations</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>Health check capabilities</li>
 * </ul>
 * </p>
 */

@Service
public class ResilientDatabaseService {
    private static final Logger log = LoggerFactory.getLogger(ResilientDatabaseService.class);
    private static final String DB_SERVICE = "databaseService";

    private final DataSource dataSource;

    public ResilientDatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes a SQL query with resilience patterns.
     * <p>
     * This method applies circuit breaker, bulkhead, and retry patterns
     * to protect against database failures.
     * </p>
     *
     * @param <T>          The return type
     * @param sql          The SQL query to execute
     * @param params       The parameters for the prepared statement
     * @param resultMapper A function to map the ResultSet to the return type
     * @return The result of the query mapped by the resultMapper
     * @throws DataAccessException If a database access error occurs
     */
    @CircuitBreaker(name = DB_SERVICE)
    @Bulkhead(name = DB_SERVICE)
    @Retry(name = DB_SERVICE)
    public <T> T executeQuery(String sql, Object[] params, Function<ResultSet, T> resultMapper) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return resultMapper.apply(rs);
            }
        } catch (SQLException e) {
            log.error("Database query failed: {}", sql, e);
            throw new DataAccessException("Database query failed", e) {
            };
        }
    }

    @CircuitBreaker(name = DB_SERVICE)
    @Bulkhead(name = DB_SERVICE)
    @Retry(name = DB_SERVICE)
    public <T> List<T> executeQueryForList(String sql, Object[] params, Function<ResultSet, T> rowMapper) {
        return executeQuery(sql, params, rs -> {
            List<T> results = new ArrayList<>();
            try {
                while (rs.next()) {
                    results.add(rowMapper.apply(rs));
                }
                return results;
            } catch (SQLException e) {
                throw new DataAccessException("Error processing result set", e) {
                };
            }
        });
    }

    /**
     * Executes a SQL update with resilience patterns.
     * <p>
     * This method applies circuit breaker, bulkhead, and retry patterns
     * to protect against database failures. It runs within a transaction.
     * </p>
     *
     * @param sql    The SQL update statement to execute
     * @param params The parameters for the prepared statement
     * @return The number of rows affected
     * @throws DataAccessException If a database access error occurs
     */
    @CircuitBreaker(name = DB_SERVICE)
    @Bulkhead(name = DB_SERVICE)
    @Retry(name = DB_SERVICE)
    @Transactional
    public int executeUpdate(String sql, Object[] params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Database update failed: {}", sql, e);
            throw new DataAccessException("Database update failed", e) {
            };
        }
    }

    @CircuitBreaker(name = DB_SERVICE)
    public <T> T executeWithFallback(Callable<T> operation, T fallbackValue) {
        try {
            return operation.call();
        } catch (Exception e) {
            log.warn("Operation failed, using fallback value", e);
            return fallbackValue;
        }
    }

    /**
     * Checks if the database connection is healthy.
     * <p>
     * Executes a simple query to verify database connectivity.
     * This method is used by health indicators to report database status.
     * </p>
     *
     * @return true if the database is accessible, false otherwise
     */
    public boolean isHealthy() {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) == 1;
                }
            }
        } catch (SQLException e) {
            log.error("Database health check failed", e);
            return false;
        }
    }
}
