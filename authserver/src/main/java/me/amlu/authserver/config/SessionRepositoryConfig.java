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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;

/**
 * Configuration for providing the FindByIndexNameSessionRepository bean.
 */
@Configuration
public class SessionRepositoryConfig {

    private final MongoDatabaseFactory mongoDatabaseFactory;

    public SessionRepositoryConfig(MongoDatabaseFactory mongoDatabaseFactory) {
        this.mongoDatabaseFactory = mongoDatabaseFactory;
    }

    /**
     * Creates and provides a MongoIndexedSessionRepository bean.
     * This is needed by UserManager for session management.
     *
     * @return A MongoIndexedSessionRepository instance
     */
    @Bean
    public MongoIndexedSessionRepository mongoIndexedSessionRepository() {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDatabaseFactory);
        return new MongoIndexedSessionRepository(mongoTemplate);
    }

    /**
     * Provides a FindByIndexNameSessionRepository bean that can be injected into services.
     *
     * @return A properly typed FindByIndexNameSessionRepository
     */
    @Bean
    public FindByIndexNameSessionRepository<? extends Session> sessionRepository() {
        return mongoIndexedSessionRepository();
    }
}
