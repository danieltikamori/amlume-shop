/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.repository;

import me.amlu.authserver.model.webauthn.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {

    /**
     * Finds a PasskeyCredential by its unique credential ID.
     *
     * @param credentialId The credential ID (often a Base64URL string).
     * @return An Optional containing the PasskeyCredential if found, or empty otherwise.
     */
    Optional<PasskeyCredential> findByCredentialId(String credentialId);

    /**
     * Finds all PasskeyCredentials associated with a given user ID.
     * Spring Data JPA will derive this query based on the 'user.id' attribute of the PasskeyCredential entity.
     *
     * @param userId The ID of the user.
     * @return A List of PasskeyCredentials belonging to the user, or an empty list if none are found.
     */
    List<PasskeyCredential> findByUserId(Long userId);

    /**
     * Finds a PasskeyCredential by its user handle.
     * Note: A user handle might not be globally unique across all users if multiple users
     * could theoretically have the same handle (though typically it's derived from the user's unique ID).
     * If userHandle is unique per user but not globally, this might return multiple if not scoped by user.
     * Assuming userHandle is unique enough for this lookup or used in a context where it's expected to be.
     *
     * @param userHandle The user handle string.
     * @return An Optional containing the PasskeyCredential if found, or empty otherwise.
     */
    Optional<PasskeyCredential> findByUserHandle(String userHandle);

    /**
     * Deletes a PasskeyCredential by its unique credential ID.
     *
     * @param credentialId The credential ID.
     */
    void deleteByCredentialId(String credentialId);

    /**
     * Deletes all PasskeyCredentials associated with a given user ID.
     *
     * @param userId The ID of the user whose credentials are to be deleted.
     */
    void deleteByUserId(Long userId);

    /**
     * Deletes a PasskeyCredential by its user handle.
     *
     * @param userHandle The user handle.
     */
    void deleteByUserHandle(String userHandle);

    /**
     * Checks if a PasskeyCredential exists with the given credential ID.
     *
     * @param credentialId The credential ID.
     * @return true if a credential exists, false otherwise.
     */
    boolean existsByCredentialId(String credentialId);

    /**
     * Checks if any PasskeyCredential exists for the given user ID.
     *
     * @param userId The ID of the user.
     * @return true if at least one credential exists for the user, false otherwise.
     */
    boolean existsByUserId(Long userId);

    /**
     * Checks if a PasskeyCredential exists with the given user handle.
     *
     * @param userHandle The user handle.
     * @return true if a credential exists, false otherwise.
     */
    boolean existsByUserHandle(String userHandle);

    // Methods like save, delete, deleteById, findById, existsById, getReferenceById (replaces getOne)
    // are already provided by JpaRepository and do not need to be redeclared here.
}