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

import me.amlu.authserver.security.SecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Audit Configuration
 * This class is responsible for configuring JPA auditing, which automatically tracks who created or modified an entity.
 * It uses the SecurityAuditorAware class to determine the current auditor (user) for auditing purposes.
 * The @EnableJpaAuditing annotation is used to enable JPA auditing in the application.
 * The auditorAwareRef attribute specifies the bean name of the AuditorAware implementation to be used.
 * In this case, it's the SecurityAuditorAware bean, which is responsible for determining the current auditor.
 * Consider using also @EnableJpaRepositories and @EnableTransactionManagement for JPA repositories and transactions, respectively.
 * Consider using also Hibernate Envers for auditing history - full entity history - if we need full historical versioning of the entity.
 */

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditConfig {
    @Bean
    public AuditorAware<Long> auditorAware() { // Assuming auditor ID is Long
        return new SecurityAuditorAware(); // Implement this class
    }
}
