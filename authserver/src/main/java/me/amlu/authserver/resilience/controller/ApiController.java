/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.resilience.controller;


import me.amlu.authserver.events.model.Event;
import me.amlu.authserver.events.service.EventServiceInterface;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for API health checks and event retrieval.
 * This controller provides endpoints for basic service health, current time,
 * and access to event data.
 *
 * <p>Usage examples:</p>
 * <ul>
 *     <li>GET /now: Get the current server time.</li>
 *     <li>GET /ping: Check if the service is responsive.</li>
 *     <li>GET /health: Check the overall health of the service.</li>
 *     <li>GET /events/all: Retrieve all events from the beginning of time.</li>
 *     <li>GET /events?from=2023-01-01T00:00:00Z: Retrieve events after a specific timestamp.</li>
 * </ul>
 */
@RestController
public class ApiController {
    private final EventServiceInterface eventService;

    /**
     * Constructs an ApiController with the given EventServiceInterface.
     *
     * @param eventService The service for managing events.
     */
    public ApiController(EventServiceInterface eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/now")
    public Instant now() {
        return Instant.now();
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @GetMapping("/events/all")
    public List<Event> getAllEvents() {
        return eventService.findEventsAfter(Instant.EPOCH);
    }

    @GetMapping("/events")
    public List<Event> getEvents(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from) {
        return eventService.findEventsAfter(from != null ? from : Instant.EPOCH);
    }
}
