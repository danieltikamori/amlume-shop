/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import me.amlu.authserver.user.model.UserMixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.jackson2.SecurityJackson2Modules;

//@Configuration
public class SecurityJacksonCustomizerConfig {
//    private static final Logger log = LoggerFactory.getLogger(SecurityJacksonCustomizerConfig.class);
//    private final PolymorphicTypeValidator sessionPolymorphicTypeValidator; // Injected
//
//    public SecurityJacksonCustomizerConfig(PolymorphicTypeValidator sessionPolymorphicTypeValidator) {
//        this.sessionPolymorphicTypeValidator = sessionPolymorphicTypeValidator;
//    }
//
//    @PostConstruct
//    public void customizeSecurityModulesObjectMapper() {
//        SecurityJackson2Modules.setObjectMapperConfigurer(om -> {
//            log.info("Customizing ObjectMapper (Hash: {}) within Spring Security modules.", System.identityHashCode(om));
//
//            // Apply the same PTV. These internal mappers will encounter @class properties
//            // written by the sessionObjectMapper, so they need this PTV to allow them.
//            // They do NOT need to activateDefaultTyping themselves, as type info is already in the JSON.
//            om.setPolymorphicTypeValidator(this.sessionPolymorphicTypeValidator);
//
//            // Register modules that the internal ObjectMapper might need to understand
//            // the structure of objects it's deserializing (e.g., User, custom VOs
//            // if they are part of Authentication.details or principal).
//            // CoreJackson2Module already registers basic security modules on 'om'.
//            om.registerModule(new JavaTimeModule());
//            om.registerModule(new Jdk8Module());
//
//            // Add mixins for your custom types if they are part of the Authentication object
//            // (e.g., User as principal, or custom VOs in Authentication.details).
//            om.addMixIn(me.amlu.authserver.user.model.User.class, me.amlu.authserver.user.model.UserMixin.class);
//            om.addMixIn(me.amlu.authserver.user.model.vo.EmailAddress.class, me.amlu.authserver.user.model.vo.EmailAddressMixin.class);
//            om.addMixIn(me.amlu.authserver.user.model.vo.AccountStatus.class, me.amlu.authserver.user.model.vo.AccountStatusMixin.class);
//            // Add other mixins for your VOs if necessary.
//
//            log.info("Applied PTV and core/custom modules/mixins to internal ObjectMapper in Spring Security modules.");
//        });
//        log.info("SecurityJackson2Modules.objectMapperConfigurer has been set. This will affect ObjectMappers created by Spring Security's Jackson modules.");
//    }
}
