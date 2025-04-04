/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.amlu.shop.amlume_shop.user_management.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "device_fingerprint", nullable = false, unique = false)
    private String deviceFingerprint;

//    // Optional
//    @Column(name = "device_id", nullable = true)
//    private String deviceId;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Override
    public void enableSoftDelete(org.hibernate.mapping.Column indicatorColumn) {
        this.deletedAt = Instant.now();
        this.revoked = true;
        this.deletedByUser = this.getUpdatedByUser();
        this.updatedAt = Instant.now();
    }

}