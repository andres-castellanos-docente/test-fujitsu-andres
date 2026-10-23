package com.fujitsu.eventprocessor.handler;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;
import com.fujitsu.eventprocessor.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventHandlersTest {

    @Mock
    private AuditService auditService;

    private final Event qualityEvent = new Event("EVT-Q1", EventType.QUALITY, "Inspection failed",
            LocalDateTime.parse("2026-05-20T10:00:00"), Instant.parse("2026-05-20T13:00:00Z"));

    @Test
    void qualityHandlerShouldGenerateAudit() {
        new QualityEventHandler(auditService).handle(qualityEvent);

        verify(auditService).registerQualityEvent(qualityEvent);
    }

    @Test
    void registryShouldResolveHandlerByType() {
        ProductionEventHandler production = new ProductionEventHandler();
        QualityEventHandler quality = new QualityEventHandler(auditService);

        EventHandlerRegistry registry = new EventHandlerRegistry(List.of(production, quality));

        assertThat(registry.getHandler(EventType.PRODUCTION)).isSameAs(production);
        assertThat(registry.getHandler(EventType.QUALITY)).isSameAs(quality);
    }

    @Test
    void registryShouldFailFastOnDuplicatedHandlers() {
        assertThatThrownBy(() -> new EventHandlerRegistry(List.of(new ProductionEventHandler(), new ProductionEventHandler())))
                .isInstanceOf(IllegalStateException.class);
    }
}
