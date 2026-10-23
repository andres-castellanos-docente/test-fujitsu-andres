package com.fujitsu.eventprocessor.handler;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Eventos PRODUCTION devem apenas ser registrados: nenhuma ação adicional.
 */
@Component
public class ProductionEventHandler implements EventTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductionEventHandler.class);

    @Override
    public EventType supportedType() {
        return EventType.PRODUCTION;
    }

    @Override
    public void handle(Event event) {
        log.debug("Evento PRODUCTION {} registrado; nenhuma ação adicional necessária", event.getEventId());
    }
}
