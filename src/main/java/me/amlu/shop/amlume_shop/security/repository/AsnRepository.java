/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.repository;

import me.amlu.shop.amlume_shop.security.model.AsnEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsnRepository extends JpaRepository<AsnEntry, String> {
    Optional<AsnEntry> findByIp(String ip);

    /**
     * Finds stale entries based on their last modification date.
     *
     * @param timestamp The cutoff timestamp. Entries last modified before this will be returned.
     * @return A list of stale AsnEntry entities.
     */
    @Query("SELECT a FROM AsnEntry a WHERE a.lastModifiedDate < :timestamp")
    List<AsnEntry> findStaleEntries(@Param("timestamp") Instant timestamp);

    /**
     * Deletes stale entries based on their last modification date.
     *
     * @param timestamp The cutoff timestamp. Entries last modified before this will be deleted.
     */
    @Modifying
    @Query("DELETE FROM AsnEntry a WHERE a.lastModifiedDate < :timestamp")
    void deleteStaleEntries(@Param("timestamp") Instant timestamp);
}
