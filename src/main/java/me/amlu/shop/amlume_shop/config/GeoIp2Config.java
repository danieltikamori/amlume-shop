/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import com.maxmind.geoip2.WebServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoIp2Config {
//    @Value("${geoip2.license.key}")
    @Value("${GEOIP2_LICENSE_KEY}")
    private String licenseKey;

//    @Value("${geoip2.account.id}")
    @Value("${GEOIP2_ACCOUNT_ID}")
    private int accountId;

    // For local MaxMind database file
//    @Value("${geoip2.database.path}")
//    private String databasePath;

    @Bean
    public WebServiceClient geoIp2Client() {
        return new WebServiceClient.Builder(accountId, licenseKey)
                .build();
    }

    // For local MaxMind database file
//    @Bean
//    public DatabaseReader geoIp2Client() throws IOException {
//        File database = new File(databasePath);
//        return new DatabaseReader.Builder(database).build();
//    }
}
