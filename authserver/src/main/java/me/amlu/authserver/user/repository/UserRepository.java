/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.repository;

import me.amlu.authserver.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail_Value(String email);

    Optional<User> findOneByEmail_ValueIgnoreCase(String email);

    Optional<User> findByExternalId(String externalId);

    boolean existsByEmail_Value(String email);

    boolean existsByBackupEmail_Value(String backupEmail);

    boolean existsByBackupEmail_ValueAndIdNot(String backupEmail, Long id);

    Optional<User> findByEmail_ValueAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);


    // Optional<User> findByName(String username); // REMOVED as 'name' field is replaced by firstName/lastName

    // Consider adding if needed:
    // Optional<User> findByFirstNameAndLastName(String firstName, String lastName);
    // List<User> findByFirstName(String firstName);
    // List<User> findByLastName(String lastName);
    // Optional<User> findByNickname(String nickname);

//    Optional<User> findByMobileNumber(String mobileNumber); // This would be findByMobileNumberE164Value
//
//    Optional<User> findByEmailOrMobileNumber(String email, String mobileNumber);

}
