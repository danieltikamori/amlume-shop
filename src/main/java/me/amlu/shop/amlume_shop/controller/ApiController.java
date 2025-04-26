/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.controller;

import me.amlu.shop.amlume_shop.service.EventServiceInterface;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class ApiController {
    private final EventServiceInterface eventService;

    public ApiController(EventServiceInterface eventService) {
        this.eventService = eventService;
    }

    public static class Event {
        public String name;
        public Instant time;

        public Event(String name, Instant time) {
            this.name = name;
            this.time = time;
        }
    }

    @GetMapping("/")
    public String index() {
        return "Hello World!";
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