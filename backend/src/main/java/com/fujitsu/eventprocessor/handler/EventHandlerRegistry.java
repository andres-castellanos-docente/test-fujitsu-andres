package com.fujitsu.eventprocessor.handler;

import com.fujitsu.eventprocessor.domain.EventType;
import com.fujitsu.eventprocessor.exception.UnsupportedEventTypeException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Localiza a estratégia correta para cada {@link EventType}.
 * Falha na inicialização se houver dois handlers para o mesmo tipo.
 */
@Component
public class EventHandlerRegistry {

    private final Map<EventType, EventTypeHandler> handlers = new EnumMap<>(EventType.class);

    public EventHandlerRegistry(List<EventTypeHandler> handlerList) {
        for (EventTypeHandler handler : handlerList) {
            EventTypeHandler previous = handlers.put(handler.supportedType(), handler);
            if (previous != null) {
                throw new IllegalStateException("Mais de um handler registrado para o tipo " + handler.supportedType());
            }
        }
    }

    public EventTypeHandler getHandler(EventType type) {
        EventTypeHandler handler = handlers.get(type);
        if (handler == null) {
            throw new UnsupportedEventTypeException(type);
        }
        return handler;
    }
}
