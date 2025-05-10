/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.model.oauth2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import me.amlu.authserver.model.vo.OAuth2AuthorizationConsentId;

@Entity
@Table(name = "oauth2_authorization_consent") // Table name used by Spring Authorization Server JDBC schema
@Getter
@Setter
@NoArgsConstructor
public class OAuth2AuthorizationConsent {

    @EmbeddedId
    private OAuth2AuthorizationConsentId id;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false) // VARCHAR(1000) in default schema
    private String authorities; // Space-delimited set of scopes/authorities

    public OAuth2AuthorizationConsent(String registeredClientId, String principalName, String authorities) {
        this.id = new OAuth2AuthorizationConsentId(registeredClientId, principalName);
        this.authorities = authorities;
    }
}