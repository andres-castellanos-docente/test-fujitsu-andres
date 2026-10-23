package com.fujitsu.eventprocessor.exception;

import com.fujitsu.eventprocessor.domain.EventType;

/** Lançada quando não existe estratégia de processamento para o tipo informado. */
public class UnsupportedEventTypeException extends RuntimeException {

    public UnsupportedEventTypeException(EventType type) {
        super("Tipo de evento não suportado: " + type);
    }
}
