/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.model.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Jackson Mixin for {@link me.amlu.authserver.user.model.vo.EmailAddress}.
 * <p>
 * This mixin is necessary to allow Jackson (and Spring Security's Jackson modules)
 * to correctly serialize and deserialize the custom {@code EmailAddress} value object,
 * especially when it's part of an entity (like {@code User}) stored in the HTTP session.
 * <p>
 * {@code @JsonTypeInfo} helps with polymorphic deserialization if needed, though for
 * simple value objects, it primarily signals to Spring Security's allowlist mechanism
 * that this type is intended for deserialization.
 * <p>
 * {@code @JsonCreator} and {@code @JsonProperty} are used to map the JSON properties
 * to the constructor parameters of the {@code EmailAddress} record.
 * <p>
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} makes deserialization more robust
 * by allowing Jackson to ignore any fields in the JSON that are not defined in the
 * Java class or this mixin. This is particularly useful for handling older data formats
 * or evolving APIs.
 * </p>
 *
 * @see me.amlu.authserver.config.JacksonConfig
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonIgnoreProperties(ignoreUnknown = true) // Add this annotation
public abstract class EmailAddressMixin {

    /**
     * Constructor for Jackson deserialization.
     * Maps the "value" JSON property to the constructor parameter of the EmailAddress record.
     *
     * @param value The email address string.
     */
    @JsonCreator
    public EmailAddressMixin(@JsonProperty("value") String value) {
        // Constructor body is not needed for mixins with records
    }
}
