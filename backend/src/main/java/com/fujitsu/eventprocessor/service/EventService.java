package com.fujitsu.eventprocessor.service;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;
import com.fujitsu.eventprocessor.dto.EventRequest;
import com.fujitsu.eventprocessor.dto.EventResponse;
import com.fujitsu.eventprocessor.exception.DuplicateEventException;
import com.fujitsu.eventprocessor.exception.EventNotFoundException;
import com.fujitsu.eventprocessor.handler.EventHandlerRegistry;
import com.fujitsu.eventprocessor.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final EventHandlerRegistry handlerRegistry;
    private final Clock clock;

    public EventService(EventRepository eventRepository, EventHandlerRegistry handlerRegistry, Clock clock) {
        this.eventRepository = eventRepository;
        this.handlerRegistry = handlerRegistry;
        this.clock = clock;
    }

    /**
     * Registra um evento garantindo que ele seja processado uma única vez.
     * <ol>
     *   <li>Verificação prévia (caminho rápido) com {@code existsByEventId}.</li>
     *   <li>Restrição única no banco cobre a condição de corrida entre requisições simultâneas.</li>
     *   <li>Delegação ao handler do tipo (ex.: QUALITY gera auditoria) na mesma transação.</li>
     * </ol>
     */
    @Transactional
    public EventResponse register(EventRequest request) {
        String eventId = request.eventId().trim();
        log.info("Recebendo evento {} do tipo {}", eventId, request.eventType());

        if (eventRepository.existsByEventId(eventId)) {
            log.warn("Evento {} já foi processado anteriormente; requisição rejeitada", eventId);
            throw new DuplicateEventException(eventId);
        }

        Event event = new Event(
                eventId,
                request.eventType(),
                request.description().trim(),
                request.timestamp(),
                Instant.now(clock));

        Event saved;
        try {
            saved = eventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Violação de unicidade ao gravar o evento {} (requisição concorrente)", eventId);
            throw new DuplicateEventException(eventId);
        }

        handlerRegistry.getHandler(saved.getEventType()).handle(saved);

        log.info("Evento {} do tipo {} processado com sucesso", eventId, saved.getEventType());
        return EventResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findAll(EventType type) {
        List<Event> events = type == null
                ? eventRepository.findAllByOrderByTimestampDesc()
                : eventRepository.findByEventTypeOrderByTimestampDesc(type);
        return events.stream().map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse findByEventId(String eventId) {
        return eventRepository.findByEventId(eventId)
                .map(EventResponse::from)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
