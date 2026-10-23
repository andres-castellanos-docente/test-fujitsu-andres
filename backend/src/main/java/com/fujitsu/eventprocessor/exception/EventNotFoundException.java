package com.fujitsu.eventprocessor.exception;

/** Lançada quando o evento consultado não existe. */
public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String eventId) {
        super("Evento '%s' não encontrado".formatted(eventId));
    }
}
