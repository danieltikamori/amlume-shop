/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class LocationInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "department")
    private String department;

    @Column(name = "region")
    private String region;


    public LocationInfo(String department, String region) {
        this.department = department == null || department.isBlank() ? "DefaultDepartment" : department;
        this.region = region == null || region.isBlank() ? "DefaultRegion" : region;
    }

    public LocationInfo() {
    }

    public static LocationInfoBuilder builder() {
        return new LocationInfoBuilder();
    }

    public String getDepartment() {
        return this.department;
    }

    public String getRegion() {
        return this.region;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof LocationInfo other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$department = this.getDepartment();
        final Object other$department = other.getDepartment();
        if (!Objects.equals(this$department, other$department))
            return false;
        final Object this$region = this.getRegion();
        final Object other$region = other.getRegion();
        return Objects.equals(this$region, other$region);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof LocationInfo;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $department = this.getDepartment();
        result = result * PRIME + ($department == null ? 43 : $department.hashCode());
        final Object $region = this.getRegion();
        result = result * PRIME + ($region == null ? 43 : $region.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "LocationInfo(department=" + this.getDepartment() + ", region=" + this.getRegion() + ")";
    }

    public static class LocationInfoBuilder {
        private String department;
        private String region;

        LocationInfoBuilder() {
        }

        public LocationInfoBuilder department(String department) {
            this.department = department;
            return this;
        }

        public LocationInfoBuilder region(String region) {
            this.region = region;
            return this;
        }

        public LocationInfo build() {
            return new LocationInfo(this.department, this.region);
        }

        @Override
        public String toString() {
            return "LocationInfo.LocationInfoBuilder(department=" + this.department + ", region=" + this.region + ")";
        }
    }
}
