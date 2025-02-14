/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.repositories;

import me.amlu.shop.amlume_shop.model.User;
import me.amlu.shop.amlume_shop.model.UserDeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceFingerprintRepository extends JpaRepository<UserDeviceFingerprint, Long> {
    List<UserDeviceFingerprint> findByUser(User user);
    long countByUser(User user);

    Optional <UserDeviceFingerprint> findByUserAndDeviceFingerprint(User user, String deviceFingerprint);

    void deleteByUserAndFingerprint(User user, String fingerprint);

    Optional<UserDeviceFingerprint> findByUserIdAndFingerprint(Long aLong, String fingerprint);

    boolean existsByUserAndFingerprint(User user, String fingerprint);
}
