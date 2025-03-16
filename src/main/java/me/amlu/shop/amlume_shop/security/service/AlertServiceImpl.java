/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.security.enums.AlertSeverityEnum;
import me.amlu.shop.amlume_shop.security.model.SecurityAlert;
import me.amlu.shop.amlume_shop.service.EnvironmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class AlertServiceImpl implements AlertService {
    private final AlertNotificationServiceImpl alertNotificationServiceImpl;
    private final MetricRegistry metricRegistry;
    private final Counter alertCounter;
    private final Timer alertTimer;
    private final EnvironmentService environment;

    @Value("${alert.max-batch-size:100}")
    private int maxBatchSize;

    public AlertServiceImpl(AlertNotificationServiceImpl alertNotificationServiceImpl, MetricRegistry metricRegistry, EnvironmentService environment) {
        this.alertNotificationServiceImpl = alertNotificationServiceImpl;
        this.metricRegistry = metricRegistry;
        this.alertCounter = metricRegistry.counter("security.alerts.total");
        this.alertTimer = metricRegistry.timer("security.alerts.processing-time");
        this.environment = environment;
    }

    @Override
    public void sendAlert(SecurityAlert alert) {
        try (Timer.Context timerContext = alertTimer.time()) {
            if (alert == null) {
                log.warn("Null alert received");
                return;
            }

            log.info("Processing security alert: {}", alert.getType());
            alertCounter.inc();

            processAlert(alert);
        } catch (Exception e) {
            log.error("Error processing alert: {}", alert, e);
            metricRegistry.counter("security.alerts.errors").inc();
        }
    }

//    @Override
//    public void sendAlerts(SecurityAlert... alerts) {
//        if (alerts == null || alerts.length == 0) {
//            log.warn("Null or empty alerts array received");
//            return;
//        }
//
//        for (SecurityAlert alert : alerts) {
//            sendAlert(alert);
//        }
//    }

    @Override
    public void alertSecurityTeam(SecurityAlert alert) {
        try (Timer.Context timerContext = alertTimer.time()) {
            if (alert == null) {
                log.warn("Null alert received");
                return;
            }

            log.info("Processing security alert: {}", alert.getType());
            alertCounter.inc();

            processAlert(alert);
        } catch (Exception e) {
            log.error("Error processing alert: {}", alert, e);
            metricRegistry.counter("security.alerts.errors").inc();
        }
    }

    @Override
    public void processAlert(SecurityAlert alert) {
        // Enrich alert with additional context
        enrichAlert(alert);

        // Determine alert severity and route accordingly
        if (alert.getSeverity() == AlertSeverityEnum.HIGH) {
            alertNotificationServiceImpl.sendUrgentNotification(alert);
        } else {
            alertNotificationServiceImpl.sendNotification(alert);
        }
    }

    @Override
    public void enrichAlert(SecurityAlert alert) {
        alert.setTimestamp(Instant.now());
        alert.setEnvironment(environment.getCurrentEnvironment());
        // Add any additional context needed
    }
}

