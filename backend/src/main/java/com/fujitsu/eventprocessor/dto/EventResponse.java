package com.fujitsu.eventprocessor.dto;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;

import java.time.Instant;
import java.time.LocalDateTime;

public record EventResponse(
        String eventId,
        EventType eventType,
        String description,
        LocalDateTime timestamp,
        Instant receivedAt
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getDescription(),
                event.getTimestamp(),
                event.getReceivedAt());
    }
}
