package com.fujitsu.eventprocessor.web;

import com.fujitsu.eventprocessor.domain.EventType;
import com.fujitsu.eventprocessor.dto.EventRequest;
import com.fujitsu.eventprocessor.dto.EventResponse;
import com.fujitsu.eventprocessor.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Eventos", description = "Recebimento e consulta de eventos")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Registra um novo evento")
    @ApiResponse(responseCode = "201", description = "Evento registrado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON malformado")
    @ApiResponse(responseCode = "409", description = "Evento já processado (duplicado)")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{eventId}")
                .buildAndExpand(response.eventId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista os eventos registrados (mais recentes primeiro)")
    public List<EventResponse> list(@RequestParam(required = false) EventType type) {
        return eventService.findAll(type);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Consulta um evento pelo eventId")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    public EventResponse get(@PathVariable String eventId) {
        return eventService.findByEventId(eventId);
    }
}
