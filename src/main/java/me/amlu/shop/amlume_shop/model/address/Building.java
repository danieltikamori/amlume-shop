/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model.address;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Embeddable
public class Building implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Size(min = 5, max = 250, message = "Building must be between 5 and 250 characters")
    @Column(name = "building_name")
    private String value;

    protected Building() {
    }

    public Building(String value) {
        if (value == null || value.trim().length() < 5 || value.trim().length() > 250) {
            throw new IllegalArgumentException("Building must be between 5 and 250 characters");
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
    // Constructor, getter
}