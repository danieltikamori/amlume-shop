/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "revoked_token", indexes = {
//        @Index(name = "idx_revoked_token_username", columnList = "username"),
        @Index(name = "idx_revoked_token_token_id", columnList = "token_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RevokedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "token_id", nullable = false)
    private String tokenId;

//    @Column(name = "username", nullable = false)
//    private String username;
//
//    @Column(name = "expiration_date", nullable = false)
//    private Instant expirationDate;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "reason", nullable = false)
    private String reason;

    public RevokedToken(String tokenId, String s) {
    }
}

