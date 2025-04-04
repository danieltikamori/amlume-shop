/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@ConfigurationProperties(prefix = "paseto")
@Component
public class PasetoProperties {

    private Duration accessLocalExpiration;
    private Duration accessPublicExpiration;
    private Duration refreshLocalExpiration;
    private Duration refreshPublicExpiration;
    private String accessPublicKey;
    private String refreshPublicKey;
    private String accessPrivateKey;
    private String refreshPrivateKey;
    private String accessSecretKey;
    private String refreshSecretKey;
    private String accessPublicKid;
    private String accessLocalKid;
    private String refreshLocalKid;

}
