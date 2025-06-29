/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.model;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import me.amlu.authserver.model.BaseEntity;
import me.amlu.authserver.util.TsidKeyGenerator;

import java.io.Serial;
import java.util.Objects;

@Getter
@Entity
@Table(name = "ip_whitelist", indexes = {
        @Index(name = "idx_ip_address", columnList = "ip_address", unique = true)
})
public class IpWhitelist extends BaseEntity<String> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Tsid
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @NotBlank(message = "IP address is required")
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "active", nullable = false)
    private boolean active;

    @NotBlank(message = "Description is required")
    @Column(name = "description", nullable = false)
    private String description;

    public IpWhitelist(String id, @NotBlank(message = "IP address is required") String ipAddress, boolean active, @NotBlank(message = "Description is required") String description) {
        this.id = (id == null || id.isEmpty()) ? TsidKeyGenerator.next() : id;
        this.ipAddress = ipAddress;
        this.active = active;
        this.description = description;
    }

    public IpWhitelist() {
    }

    public static IpWhitelistBuilder builder() {
        return new IpWhitelistBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpWhitelist that)) return false;
        return active == that.active && Objects.equals(id, that.id) && Objects.equals(ipAddress, that.ipAddress) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ipAddress, active, description);
    }

    @Override
    public String getAuditableId() {
        return id;
    }

    public static class IpWhitelistBuilder {
        private String id;
        private @NotBlank(message = "IP address is required") String ipAddress;
        private boolean active;
        private @NotBlank(message = "Description is required") String description;

        IpWhitelistBuilder() {
        }

        public IpWhitelistBuilder id(String id) {
            this.id = (id == null || id.isEmpty()) ? TsidKeyGenerator.next() : id;
            return this;
        }

        public IpWhitelistBuilder ipAddress(@NotBlank(message = "IP address is required") String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public IpWhitelistBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public IpWhitelistBuilder description(@NotBlank(message = "Description is required") String description) {
            this.description = description;
            return this;
        }

        public IpWhitelist build() {
            return new IpWhitelist(this.id, this.ipAddress, this.active, this.description);
        }

        public String toString() {
            return "IpWhitelist.IpWhitelistBuilder(id=" + this.id + ", ipAddress=" + this.ipAddress + ", active=" + this.active + ", description=" + this.description + ")";
        }
    }
}
