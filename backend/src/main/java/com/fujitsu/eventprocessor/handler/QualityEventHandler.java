package com.fujitsu.eventprocessor.handler;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;
import com.fujitsu.eventprocessor.service.AuditService;
import org.springframework.stereotype.Component;

/**
 * Eventos QUALITY são registrados e também geram um registro de auditoria.
 */
@Component
public class QualityEventHandler implements EventTypeHandler {

    private final AuditService auditService;

    public QualityEventHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public EventType supportedType() {
        return EventType.QUALITY;
    }

    @Override
    public void handle(Event event) {
        auditService.registerQualityEvent(event);
    }
}
