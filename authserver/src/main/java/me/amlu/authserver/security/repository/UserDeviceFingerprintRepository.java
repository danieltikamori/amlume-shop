/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.repository;

import me.amlu.authserver.security.model.UserDeviceFingerprint;
import me.amlu.authserver.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing UserDeviceFingerprint entities.
 *
 * <p>This repository provides methods to store, retrieve, and manage device fingerprints
 * associated with users. It extends JpaRepository to inherit standard CRUD operations.</p>
 */
@Repository
public interface UserDeviceFingerprintRepository extends JpaRepository<UserDeviceFingerprint, Long> {

    /**
     * Finds all device fingerprints for a specific user.
     *
     * @param user the user
     * @return a list of device fingerprints
     */
    List<UserDeviceFingerprint> findByUser(User user);

    /**
     * Counts the number of device fingerprints for a specific user.
     *
     * @param user the user
     * @return the count of device fingerprints
     */
    long countByUser(User user);

    /**
     * Finds a device fingerprint by user and fingerprint value.
     *
     * @param user              the user
     * @param deviceFingerprint the fingerprint value
     * @return an Optional containing the device fingerprint if found
     */
    Optional<UserDeviceFingerprint> findByUserAndDeviceFingerprint(User user, String deviceFingerprint);

    /**
     * Deletes a device fingerprint by user and fingerprint value.
     *
     * @param user        the user
     * @param fingerprint the fingerprint value
     */
    void deleteByUserAndDeviceFingerprint(User user, String fingerprint);

    /**
     * Finds a device fingerprint by user ID and fingerprint value.
     *
     * @param userId      the user ID
     * @param fingerprint the fingerprint value
     * @return an Optional containing the device fingerprint if found
     */
    Optional<UserDeviceFingerprint> findByUserIdAndDeviceFingerprint(Long userId, String fingerprint);

    /**
     * Checks if a device fingerprint exists for a specific user.
     *
     * @param user        the user
     * @param fingerprint the fingerprint value
     * @return true if the device fingerprint exists, false otherwise
     */
    boolean existsByUserAndDeviceFingerprint(User user, String fingerprint);

    /**
     * Finds all active device fingerprints for a specific user.
     *
     * @param user the user
     * @return a list of active device fingerprints
     */
    List<UserDeviceFingerprint> findByUserAndActiveTrue(User user);

    /**
     * Finds all device fingerprints for a user except the specified one.
     *
     * @param userId            the user ID
     * @param exceptFingerprint the fingerprint to exclude
     * @return a list of device fingerprints
     */
    List<UserDeviceFingerprint> findByUserIdAndDeviceFingerprintNot(Long userId, String exceptFingerprint);

    /**
     * Counts the number of active device fingerprints for a specific user.
     *
     * @param user the user
     * @return the count of active device fingerprints
     */
    long countByUserAndActiveTrue(User user);

    /**
     * Checks if a device fingerprint exists for a specific user ID.
     *
     * @param userId the user ID
     * @param id     the device fingerprint ID
     * @return true if the device fingerprint exists for the user, false otherwise
     */
    boolean existsByUser_IdAndId(Long userId, Long id);

    /**
     * Deletes a device fingerprint by user ID and fingerprint value.
     *
     * @param userId      the user ID
     * @param fingerprint the fingerprint value
     */
    void deleteDeviceFingerprintAndUserId(Long userId, String fingerprint);

    /**
     * Deletes a device fingerprint by ID and user ID.
     *
     * @param id     the device fingerprint ID
     * @param userId the user ID
     */
    void deleteDeviceFingerprintIdAndUserId(Long id, Long userId);

    /**
     * Deletes ALL device fingerprints associated with the user
     * For cascading delete
     *
     * @param userId the user ID
     */
    void deleteByUserId(Long userId);
}
