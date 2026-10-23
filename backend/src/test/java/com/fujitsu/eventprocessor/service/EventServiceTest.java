package com.fujitsu.eventprocessor.service;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;
import com.fujitsu.eventprocessor.dto.EventRequest;
import com.fujitsu.eventprocessor.dto.EventResponse;
import com.fujitsu.eventprocessor.exception.DuplicateEventException;
import com.fujitsu.eventprocessor.exception.EventNotFoundException;
import com.fujitsu.eventprocessor.handler.EventHandlerRegistry;
import com.fujitsu.eventprocessor.handler.EventTypeHandler;
import com.fujitsu.eventprocessor.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-20T13:00:00Z");
    private static final LocalDateTime TIMESTAMP = LocalDateTime.parse("2026-05-20T10:00:00");

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventHandlerRegistry handlerRegistry;
    @Mock
    private EventTypeHandler handler;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, handlerRegistry, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldPersistEventAndDelegateToTypeHandler() {
        EventRequest request = new EventRequest(" EVT-1001 ", EventType.PRODUCTION, " Production line started ", TIMESTAMP);
        when(eventRepository.existsByEventId("EVT-1001")).thenReturn(false);
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(handlerRegistry.getHandler(EventType.PRODUCTION)).thenReturn(handler);

        EventResponse response = service.register(request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).saveAndFlush(captor.capture());
        Event saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo("EVT-1001");
        assertThat(saved.getDescription()).isEqualTo("Production line started");
        assertThat(saved.getReceivedAt()).isEqualTo(NOW);
        verify(handler).handle(saved);

        assertThat(response.eventId()).isEqualTo("EVT-1001");
        assertThat(response.eventType()).isEqualTo(EventType.PRODUCTION);
        assertThat(response.timestamp()).isEqualTo(TIMESTAMP);
    }

    @Test
    void shouldRejectDuplicateWithoutPersistingOrHandling() {
        EventRequest request = new EventRequest("EVT-1001", EventType.QUALITY, "Inspection", TIMESTAMP);
        when(eventRepository.existsByEventId("EVT-1001")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateEventException.class)
                .hasMessageContaining("EVT-1001");

        verify(eventRepository, never()).saveAndFlush(any());
        verifyNoInteractions(handlerRegistry);
    }

    @Test
    void shouldTranslateUniqueConstraintViolationIntoDuplicate() {
        EventRequest request = new EventRequest("EVT-1001", EventType.QUALITY, "Inspection", TIMESTAMP);
        when(eventRepository.existsByEventId("EVT-1001")).thenReturn(false);
        when(eventRepository.saveAndFlush(any(Event.class)))
                .thenThrow(new DataIntegrityViolationException("uk_events_event_id"));

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(DuplicateEventException.class);
        verifyNoInteractions(handlerRegistry);
    }

    @Test
    void shouldFilterByTypeWhenProvided() {
        Event event = new Event("EVT-2", EventType.QUALITY, "Inspection", TIMESTAMP, NOW);
        when(eventRepository.findByEventTypeOrderByTimestampDesc(EventType.QUALITY)).thenReturn(List.of(event));

        List<EventResponse> result = service.findAll(EventType.QUALITY);

        assertThat(result).extracting(EventResponse::eventId).containsExactly("EVT-2");
        verify(eventRepository, never()).findAllByOrderByTimestampDesc();
    }

    @Test
    void shouldListAllWhenTypeIsNull() {
        when(eventRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of());

        assertThat(service.findAll(null)).isEmpty();
        verify(eventRepository).findAllByOrderByTimestampDesc();
    }

    @Test
    void shouldThrowNotFoundForUnknownEventId() {
        when(eventRepository.findByEventId("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByEventId("NOPE")).isInstanceOf(EventNotFoundException.class);
    }
}
