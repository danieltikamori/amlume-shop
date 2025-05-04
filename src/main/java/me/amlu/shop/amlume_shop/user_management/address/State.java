/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management.address;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class State implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(min = 2, max = 250, message = "State or province must be between 2 and 250 characters")
    @Column(name = "state_name")
    private String stateName;

    protected State() {
    } // for JPA

    public State(String stateName) {
        // It's better to perform validation using Bean Validation annotations and let the framework handle it,
        // but constructor validation is also an option. Ensure the stateName is not null or empty before trim.
        if (stateName == null || stateName.trim().length() < 2 || stateName.trim().length() > 250) {
            // Using a custom exception related to domain validation might be better than IllegalArgumentException
            throw new IllegalArgumentException("The state name must be between 2 and 250 characters");
        }
        this.stateName = stateName.trim(); // Trim whitespace
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use getClass() for strict stateName object equality
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        // Compare the core stateName field using Objects.equals for null safety
        return Objects.equals(stateName, state.stateName);
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for concise and null-safe hashCode generation
        return Objects.hash(stateName);
    }

    @Override
    public String toString() {
        // This toString is reasonable for a simple stateName object
        return stateName;
    }

    public @NotBlank @Size(min = 2, max = 250, message = "State or province must be between 2 and 250 characters") String getStateName() {
        return this.stateName;
    }
}

