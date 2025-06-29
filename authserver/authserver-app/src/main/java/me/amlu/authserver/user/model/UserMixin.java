/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson Mixin for {@link me.amlu.authserver.user.model.User}.
 * <p>
 * This mixin is necessary to allow Jackson (and Spring Security's Jackson modules)
 * to correctly serialize and deserialize the custom {@code User} entity when it is
 * stored in the HTTP session (e.g., as a principal in the SecurityContext).
 * <p>
 * {@code @JsonTypeInfo} is crucial for polymorphic deserialization, ensuring Jackson
 * knows the concrete type to instantiate when encountering the JSON representation
 * of the {@code UserDetails} principal. {@code @JsonAutoDetect} helps Jackson
 * discover fields/getters/setters for mapping. {@code @JsonIgnoreProperties} can
 * be used to ignore fields that should not be serialized/deserialized (though
 * {@code @JsonIgnore} on the entity is often preferred).
 * <p>
 * By registering this mixin with the {@link ObjectMapper}, we explicitly allow
 * the {@code me.amlu.authserver.user.model.User} class to be deserialized,
 * addressing the "not in the allowlist" error.
 * </p>
 *
 * @see me.amlu.authserver.config.JacksonConfig
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true) // Good practice for robustness against future field changes

// If your User class has complex constructors or immutable fields that Jackson
// struggles with, you might need custom deserializers/serializers.
// @JsonDeserialize(using = UserDeserializer.class) // Example if needed
// @JsonSerialize(using = UserSerializer.class) // Example if needed

// Add @JsonProperty annotations here if specific fields need explicit mapping,
// especially if they don't match standard bean property names or are constructor parameters.
// For a typical JPA entity implementing UserDetails, default field/getter mapping might work
// once the type is allowed via @JsonTypeInfo and mixin registration.
public abstract class UserMixin {

    // Example if you need to explicitly map fields or constructor parameters:
    // @JsonProperty("id")
    // private Long id;
    // @JsonProperty("email")
    // private EmailAddress email;
    // @JsonProperty("roles")
    // abstract Collection<? extends GrantedAuthority> getRoles(); // Map the getter if needed

    // If your User class has a constructor used for deserialization,
    // you might need @JsonCreator and @JsonProperty on the constructor parameters in the Mixin.
    // Example (assuming a constructor like User(Long id, String externalId, ...)):
    // @JsonCreator
    // UserMixin(
    //     @JsonProperty("id") Long id,
    //     @JsonProperty("externalId") String externalId,
    //     // Add other constructor parameters with @JsonProperty
    //     ...
    // );

    // For a simple UserDetails implementation with default field visibility,
    // the @JsonTypeInfo and mixin registration might be enough.
}
