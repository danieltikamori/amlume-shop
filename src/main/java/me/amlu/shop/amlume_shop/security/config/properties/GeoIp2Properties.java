/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * Configuration properties for GeoIP2 database.
 * <p>
 * This class is used to bind the properties defined in the application configuration file (e.g., application.yml or application.properties)
 * to a Java object. It uses Spring Boot's @ConfigurationProperties annotation to achieve this.
 * <p>
 * The properties are validated using Jakarta Bean Validation annotations.
 */
@Configuration
@ConfigurationProperties(prefix = "geoip2")
@Validated
public class GeoIp2Properties {

    @NotBlank
    private String downloadPath; // e.g., /tmp/geoip-updates

    @NotNull
    private License license;

    @NotBlank
    private String databaseDirectory; // e.g., C:/dev/geoip-databases

    @NotNull
    private DatabaseConfig cityDatabase;
    @NotNull
    private DatabaseConfig asnDatabase;
    @NotNull
    private DatabaseConfig countryDatabase;

    public GeoIp2Properties() {
    }

    public @NotBlank String getDownloadPath() {
        return this.downloadPath;
    }

    public @NotNull License getLicense() {
        return this.license;
    }

    public @NotBlank String getDatabaseDirectory() {
        return this.databaseDirectory;
    }

    public @NotNull DatabaseConfig getCityDatabase() {
        return this.cityDatabase;
    }

    public @NotNull DatabaseConfig getAsnDatabase() {
        return this.asnDatabase;
    }

    public @NotNull DatabaseConfig getCountryDatabase() {
        return this.countryDatabase;
    }

    public void setDownloadPath(@NotBlank String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public void setLicense(@NotNull License license) {
        this.license = license;
    }

    public void setDatabaseDirectory(@NotBlank String databaseDirectory) {
        this.databaseDirectory = databaseDirectory;
    }

    public void setCityDatabase(@NotNull DatabaseConfig cityDatabase) {
        this.cityDatabase = cityDatabase;
    }

    public void setAsnDatabase(@NotNull DatabaseConfig asnDatabase) {
        this.asnDatabase = asnDatabase;
    }

    public void setCountryDatabase(@NotNull DatabaseConfig countryDatabase) {
        this.countryDatabase = countryDatabase;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof GeoIp2Properties other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$downloadPath = this.getDownloadPath();
        final Object other$downloadPath = other.getDownloadPath();
        if (!Objects.equals(this$downloadPath, other$downloadPath))
            return false;
        final Object this$license = this.getLicense();
        final Object other$license = other.getLicense();
        if (!Objects.equals(this$license, other$license)) return false;
        final Object this$databaseDirectory = this.getDatabaseDirectory();
        final Object other$databaseDirectory = other.getDatabaseDirectory();
        if (!Objects.equals(this$databaseDirectory, other$databaseDirectory))
            return false;
        final Object this$cityDatabase = this.getCityDatabase();
        final Object other$cityDatabase = other.getCityDatabase();
        if (!Objects.equals(this$cityDatabase, other$cityDatabase))
            return false;
        final Object this$asnDatabase = this.getAsnDatabase();
        final Object other$asnDatabase = other.getAsnDatabase();
        if (!Objects.equals(this$asnDatabase, other$asnDatabase))
            return false;
        final Object this$countryDatabase = this.getCountryDatabase();
        final Object other$countryDatabase = other.getCountryDatabase();
        return Objects.equals(this$countryDatabase, other$countryDatabase);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof GeoIp2Properties;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $downloadPath = this.getDownloadPath();
        result = result * PRIME + ($downloadPath == null ? 43 : $downloadPath.hashCode());
        final Object $license = this.getLicense();
        result = result * PRIME + ($license == null ? 43 : $license.hashCode());
        final Object $databaseDirectory = this.getDatabaseDirectory();
        result = result * PRIME + ($databaseDirectory == null ? 43 : $databaseDirectory.hashCode());
        final Object $cityDatabase = this.getCityDatabase();
        result = result * PRIME + ($cityDatabase == null ? 43 : $cityDatabase.hashCode());
        final Object $asnDatabase = this.getAsnDatabase();
        result = result * PRIME + ($asnDatabase == null ? 43 : $asnDatabase.hashCode());
        final Object $countryDatabase = this.getCountryDatabase();
        result = result * PRIME + ($countryDatabase == null ? 43 : $countryDatabase.hashCode());
        return result;
    }

    public String toString() {
        return "GeoIp2Properties(downloadPath=" + this.getDownloadPath() + ", license=" + this.getLicense() + ", databaseDirectory=" + this.getDatabaseDirectory() + ", cityDatabase=" + this.getCityDatabase() + ", asnDatabase=" + this.getAsnDatabase() + ", countryDatabase=" + this.getCountryDatabase() + ")";
    }

    public static class License {
        @NotBlank
        private String accountId;
        @NotBlank
        private String licenseKey;

        public License() {
        }

        public @NotBlank String getAccountId() {
            return this.accountId;
        }

        public @NotBlank String getLicenseKey() {
            return this.licenseKey;
        }

        public void setAccountId(@NotBlank String accountId) {
            this.accountId = accountId;
        }

        public void setLicenseKey(@NotBlank String licenseKey) {
            this.licenseKey = licenseKey;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof License other)) return false;
            if (!other.canEqual((Object) this)) return false;
            final Object this$accountId = this.getAccountId();
            final Object other$accountId = other.getAccountId();
            if (!Objects.equals(this$accountId, other$accountId))
                return false;
            final Object this$licenseKey = this.getLicenseKey();
            final Object other$licenseKey = other.getLicenseKey();
            return Objects.equals(this$licenseKey, other$licenseKey);
        }

        protected boolean canEqual(final Object other) {
            return other instanceof License;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $accountId = this.getAccountId();
            result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
            final Object $licenseKey = this.getLicenseKey();
            result = result * PRIME + ($licenseKey == null ? 43 : $licenseKey.hashCode());
            return result;
        }

        public String toString() {
            return "GeoIp2Properties.License(accountId=" + this.getAccountId() + ", licenseKey=" + this.getLicenseKey() + ")";
        }
    }

    public static class DatabaseConfig {
        @NotBlank
        private String path; // Will resolve based on databaseDirectory

        public DatabaseConfig() {
        }

        public @NotBlank String getPath() {
            return this.path;
        }

        public void setPath(@NotBlank String path) {
            this.path = path;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof DatabaseConfig other)) return false;
            if (!other.canEqual((Object) this)) return false;
            final Object this$path = this.getPath();
            final Object other$path = other.getPath();
            return Objects.equals(this$path, other$path);
        }

        protected boolean canEqual(final Object other) {
            return other instanceof DatabaseConfig;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $path = this.getPath();
            result = result * PRIME + ($path == null ? 43 : $path.hashCode());
            return result;
        }

        public String toString() {
            return "GeoIp2Properties.DatabaseConfig(path=" + this.getPath() + ")";
        }
    }
}
