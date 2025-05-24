/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.oauth2.repository;

import me.amlu.authserver.oauth2.model.OAuth2AuthorizationConsent;
import me.amlu.authserver.user.model.vo.OAuth2AuthorizationConsentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Secured("ROLE_USER")
@Repository
public interface OAuth2AuthorizationConsentRepository extends JpaRepository<OAuth2AuthorizationConsent, OAuth2AuthorizationConsentId> {

    /**
     * Finds an OAuth2AuthorizationConsent by registered client ID and principal name.
     *
     * @param registeredClientId the ID of the registered client
     * @param principalName      the name of the principal
     * @return an Optional containing the found OAuth2AuthorizationConsent, or empty if not found
     */
    Optional<OAuth2AuthorizationConsent> findByIdRegisteredClientIdAndIdPrincipalName(String registeredClientId, String principalName);

    /**
     * Finds an OAuth2AuthorizationConsent by the principal name.
     *
     * @param principalName the name of the principal
     * @return an Optional containing the found OAuth2AuthorizationConsent, or empty if not found
     */
    Optional<OAuth2AuthorizationConsent> findByIdPrincipalName(String principalName);

    @Override
//    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_ROOT"})
    void delete(@NonNull OAuth2AuthorizationConsent entity);

    @Override
//    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_ROOT"})
    void deleteById(@NonNull OAuth2AuthorizationConsentId id);

    //    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_ROOT"})
    void deleteByIdRegisteredClientIdAndIdPrincipalName(String registeredClientId, String principalName);

    //    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_ROOT"})
    void deleteByIdPrincipalName(@NonNull String principalName);

}
