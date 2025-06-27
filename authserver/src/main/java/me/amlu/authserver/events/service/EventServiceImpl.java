/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.events.service;

import me.amlu.authserver.events.model.Event;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventServiceInterface {

    private final List<Event> events = List.of(
            new Event("Event 1", Instant.parse("2024-01-01T00:00:00Z")),
            new Event("Event 2", Instant.parse("2024-01-02T00:00:00Z")),
            new Event("Event 3", Instant.parse("2024-01-03T00:00:00Z"))
    );

    @Override
    public List<Event> findEventsAfter(Instant from) {
        return events.stream().filter(event -> event.getTimestamp().isAfter(from)).collect(Collectors.toList());
    }
}
