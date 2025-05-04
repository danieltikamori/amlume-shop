/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service.util;

import me.amlu.shop.amlume_shop.security.service.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class DatabaseSecretsLoader {

    //    @Value("${vault.secret.database-path:secret/data/amlume-shop/database}")
    @Value("${spring.cloud.vault.path:secret/amlume-shop/mfa}")
    String vaultPath;

    private final VaultService vaultService;

    private static final Logger log = LoggerFactory.getLogger(DatabaseSecretsLoader.class);

    public DatabaseSecretsLoader(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    // Example usage
//    public void loadDbConfig(int configVersion) {
//
//        Optional<DatabaseConfig> dbConfigOpt = vaultService.getSpecificVersionedSecretAsType(
//                vaultPath,
//                configVersion,
//                DatabaseConfig.class // Pass the class type
//        );
//
//        if (dbConfigOpt.isPresent()) {
//            DatabaseConfig config = dbConfigOpt.get();
//            System.out.println("DB URL: " + config.url()); // Access fields
//            // Use the config object...
//        } else {
//            // Handle missing config
//            log.error("Database config version {} not found or mapping failed.", configVersion);
//        }
//    }

}
