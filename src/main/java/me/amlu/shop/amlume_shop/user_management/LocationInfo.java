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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
@ToString
@Embeddable
@NoArgsConstructor(force = true)
public class LocationInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "department")
    private final String department;

    @Column(name = "region")
    private final String region;


    public LocationInfo(String department, String region) {
        this.department = department == null || department.isBlank() ? "DefaultDepartment" : department;
        this.region = region == null || region.isBlank() ? "DefaultRegion" : region;
    }

}