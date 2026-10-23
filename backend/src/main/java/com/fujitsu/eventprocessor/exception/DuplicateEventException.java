package com.fujitsu.eventprocessor.exception;

/** Lançada quando um evento com o mesmo eventId já foi processado. */
public class DuplicateEventException extends RuntimeException {

    private final String eventId;

    public DuplicateEventException(String eventId) {
        super("O evento '%s' já foi processado anteriormente".formatted(eventId));
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
