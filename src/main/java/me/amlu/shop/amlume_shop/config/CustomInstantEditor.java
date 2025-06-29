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

import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Custom property editor for Instant
 */
public class CustomInstantEditor extends PropertyEditorSupport {
    
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            try {
                setValue(Instant.parse(text));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid Instant format: " + text, e);
            }
        } else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        Instant value = (Instant) getValue();
        return value != null ? value.toString() : "";
    }
}

/*
Usage:
@RestController
public class ApiController {
    @GetMapping("/events")
    public List<Event> getEvents(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from) {
        return eventService.findEventsAfter(from != null ? from : Instant.EPOCH);
    }
}
@RestController
public class ApiController {
    @PostMapping("/events")
    public Event createEvent(@RequestBody Event event) {
        return eventService.saveEvent(event);
    }
}

When you need to display times in a specific timezone:

public String formatForDisplay(Instant instant, ZoneId zoneId) {
    return instant
        .atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
}

 */
