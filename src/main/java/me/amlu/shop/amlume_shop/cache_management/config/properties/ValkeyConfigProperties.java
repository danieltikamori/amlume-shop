/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.cache_management.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Configuration properties for Valkey (Redis fork) connection and pooling.
 * Maps properties under the 'valkey' prefix (e.g., valkey.host, valkey.port, valkey.password, valkey.pool.*).
 * <p>
 * IMPORTANT: Ensure the 'password' property is populated securely, ideally from Vault via Spring Cloud Vault
 * in production environments, or via environment variables. Avoid committing default passwords.
 * <p>
 * This configuration assumes a standalone Valkey instance using host/port.
 * For cluster configuration, the 'nodes' property and related logic would need to be added/enabled.
 */
@Component // Make it a component so it can be injected
@Validated // Enable validation if constraints are added
@ConfigurationProperties(prefix = "valkey") // Valkey Configuration
public class ValkeyConfigProperties implements RedisConfigPropertiesInterface { // Keep interface if used elsewhere

// Option 1: Use nodes for cluster configuration (if applicable)
    // private String nodes; // Example: "host1:port1,host2:port2,host3:port3"
    // Uncomment if using cluster and remove host/port

    // Option 2: Add individual host/port if using standalone (preferred for clarity if standalone)
    // --- Standalone Connection Properties ---
    @NotNull(message = "Valkey host cannot be null")
    private String host;

    @Min(value = 1, message = "Valkey port must be a positive integer")
    private int port;

    @NotNull(message = "Valkey password cannot be null")
    private String password;

    // --- Nested Pool Properties ---
    @Valid // Enable validation for the nested Pool object
    private Pool pool = new Pool(); // Initialize nested properties

    // --- SSL/TLS Properties ---
    @Valid // Enable validation for nested SSL properties
    private Ssl ssl = new Ssl(); // Add nested SSL properties


    public ValkeyConfigProperties() {
    }

    // --- Getters and Setters ---

    // Implement interface methods (adjust based on whether you use nodes or host/port)
//    @Override
//    public String getNodes() {
//        // Return 'nodes' or construct from host/port if needed by interface consumer
//        return nodes != null ? nodes : (host != null ? host + ":" + port : null);
//    }

    @Override
    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(@NotNull String password) {
        this.password = password;
    }

    public Pool getPool() {
        return pool;
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public Ssl getSsl() {
        return ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }



    // --- Nested Static Class for Pool Configuration ---
    @Validated
    public static class Pool {

        // Match properties from application.yml under valkey.pool
        @Min(0)
        private int maxActive = 8; // Default from Lettuce
        @Min(0)
        private int maxIdle = 8;   // Default from Lettuce
        @Min(0)
        private int minIdle = 0;   // Default from Lettuce

        @DurationUnit(ChronoUnit.MILLIS)
        private Duration maxWait = Duration.ofMillis(-1); // Default from Lettuce (-1 means block indefinitely)

        // Add other pool properties from application.yml if needed
        // private Duration minEvictableIdleTime;
        // private Integer numTestsPerEvictionRun;
        // private Duration timeBetweenEvictionRuns;
        // private Boolean testWhileIdle;
        // ... etc.

        // --- Getters and Setters for Pool ---

        public int getMaxActive() {
            return maxActive;
        }

        public void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }

        public int getMaxIdle() {
            return maxIdle;
        }

        public void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }

        public int getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public Duration getMaxWait() {
            return maxWait;
        }

        public void setMaxWait(Duration maxWait) {
            this.maxWait = maxWait;
        }

        // --- equals, hashCode, toString for Pool ---
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pool pool = (Pool) o;
            return maxActive == pool.maxActive &&
                    maxIdle == pool.maxIdle &&
                    minIdle == pool.minIdle &&
                    Objects.equals(maxWait, pool.maxWait);
        }

        @Override
        public int hashCode() {
            return Objects.hash(maxActive, maxIdle, minIdle, maxWait);
        }

        @Override
        public String toString() {
            return "Pool{" +
                    "maxActive=" + maxActive +
                    ", maxIdle=" + maxIdle +
                    ", minIdle=" + minIdle +
                    ", maxWait=" + maxWait +
                    '}';
        }
    }

    // --- Nested Static Class for SSL Configuration ---
    @Validated
    public static class Ssl {
        private boolean enabled = true; // Default to true since you enabled it in code

        @Valid // Validate nested truststore properties
        private Truststore truststore = new Truststore();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Truststore getTruststore() {
            return truststore;
        }

        public void setTruststore(Truststore truststore) {
            this.truststore = truststore;
        }

        @Override
        public String toString() {
            return "Ssl{" +
                    "enabled=" + enabled +
                    ", truststore=" + truststore +
                    '}';
        }

        // --- Nested Static Class for Truststore Configuration ---
        @Validated
        public static class Truststore {
            // Use Resource for flexibility (classpath: or file:)
            private Resource path; // e.g., file:./config/truststore.jks or classpath:truststore.jks

            // Password should be loaded securely (e.g., from Vault/Env)
            private String password;

            public Resource getPath() {
                return path;
            }

            public void setPath(Resource path) {
                this.path = path;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            @Override
            public String toString() {
                return "Truststore{" +
                        "path=" + (path != null ? path.getDescription() : "null") +
                        ", password=" + (password != null ? "[REDACTED]" : "null") +
                        '}';
            }
        }
    }

    // --- Update equals, hashCode, toString in ValkeyConfigProperties ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use pattern matching for instanceof (requires Java 16+)
        if (!(o instanceof ValkeyConfigProperties that)) return false;

        // Compare fields
        return port == that.port &&
                Objects.equals(host, that.host) &&
                Objects.equals(password, that.password) &&
                Objects.equals(pool, that.pool) && // Include pool
                Objects.equals(ssl, that.ssl); // Include ssl
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, password, pool, ssl); // Include ssl
    }

    @Override
    public String toString() {
        // Avoid logging password directly
        return "ValkeyConfigProperties(" +
                "host=" + host +
                ", port=" + port +
                ", password=" + (password != null ? "[REDACTED]" : "null") +
                ", pool=" + pool +
                ", ssl=" + ssl + // Include ssl
                ')';
    }

    // If using nodes, implement the logic to parse and set host/port from nodes
    // Add getters/setters for host/port if using Option 2
    // public String getHost() { return host; }
    // public void setHost(String host) { this.host = host; }
    // public int getPort() { return port; }
    // public void setPort(int port) { this.port = port; }
    // If using host/port, implement the interface method accordingly
}