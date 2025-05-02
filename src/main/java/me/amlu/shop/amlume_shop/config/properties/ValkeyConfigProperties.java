/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

// TODO: Ensure this bean is populated securely, ideally from Vault via Spring Cloud Vault.
// TODO: Verify if 'nodes' property is still needed or if 'host' and 'port' are sufficient.
//       If 'nodes' is used (e.g., for cluster), parsing logic in ValkeyCacheConfig might be needed.

@Component // Make it a component so it can be injected
@Validated // Enable validation if constraints are added
@ConfigurationProperties(prefix = "valkey") // Valkey Configuration
public class ValkeyConfigProperties implements RedisConfigPropertiesInterface { // Keep interface if used elsewhere

// Option 1: Use nodes for cluster configuration (if applicable)
    // private String nodes; // Example: "host1:port1,host2:port2,host3:port3"
    // Uncomment if using cluster and remove host/port

    // Option 2: Add individual host/port if using standalone (preferred for clarity if standalone)
    private String host; // Add host field
    private int port;    // Add port field

    @NotNull // Add validation if password is required
    private String password;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ValkeyConfigProperties other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$host = this.getHost();
        final Object other$host = other.getHost();
        if (!Objects.equals(this$host, other$host)) return false;
        if (this.getPort() != other.getPort()) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        return Objects.equals(this$password, other$password);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ValkeyConfigProperties;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $host = this.getHost();
        result = result * PRIME + ($host == null ? 43 : $host.hashCode());
        result = result * PRIME + this.getPort();
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        return result;
    }

    public String toString() {
        return "ValkeyConfigProperties(host=" + this.getHost() + ", port=" + this.getPort() + ", password=" + this.getPassword() + ")";
    }

    // If using nodes, implement the logic to parse and set host/port from nodes
    // Add getters/setters for host/port if using Option 2
    // public String getHost() { return host; }
    // public void setHost(String host) { this.host = host; }
    // public int getPort() { return port; }
    // public void setPort(int port) { this.port = port; }
    // If using host/port, implement the interface method accordingly
}