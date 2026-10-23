package com.fujitsu.eventprocessor.web;

import com.fujitsu.eventprocessor.domain.EventType;
import com.fujitsu.eventprocessor.dto.EventRequest;
import com.fujitsu.eventprocessor.dto.EventResponse;
import com.fujitsu.eventprocessor.exception.DuplicateEventException;
import com.fujitsu.eventprocessor.exception.EventNotFoundException;
import com.fujitsu.eventprocessor.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    private static final String VALID_BODY = """
            {"eventId":"EVT-1001","eventType":"PRODUCTION","description":"Production line started","timestamp":"2026-05-20T10:00:00"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private final EventResponse response = new EventResponse("EVT-1001", EventType.PRODUCTION,
            "Production line started", LocalDateTime.parse("2026-05-20T10:00:00"), Instant.parse("2026-05-20T13:00:00Z"));

    @Test
    void postShouldReturn201WithLocationAndCorrelationId() throws Exception {
        when(eventService.register(any(EventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/events/EVT-1001"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.eventType").value("PRODUCTION"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-20T10:00:00"));
    }

    @Test
    void postDuplicateShouldReturn409ProblemDetail() throws Exception {
        when(eventService.register(any(EventRequest.class))).thenThrow(new DuplicateEventException("EVT-1001"));

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Evento duplicado"))
                .andExpect(jsonPath("$.eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void postWithMissingFieldsShouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.eventId").exists())
                .andExpect(jsonPath("$.errors.eventType").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.timestamp").exists());

        verifyNoInteractions(eventService);
    }

    @Test
    void postWithInvalidEventTypeShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"EVT-1\",\"eventType\":\"MAINTENANCE\",\"description\":\"x\",\"timestamp\":\"2026-05-20T10:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição malformada"));
    }

    @Test
    void postWithMalformedJsonShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postWithUnsupportedContentTypeShouldReturn415() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.TEXT_PLAIN).content(VALID_BODY))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.correlationId").exists());

        verifyNoInteractions(eventService);
    }

    @Test
    void unsupportedMethodShouldReturn405() throws Exception {
        mockMvc.perform(put("/api/events").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void getShouldListEventsFilteredByType() throws Exception {
        when(eventService.findAll(EventType.PRODUCTION)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/events").param("type", "PRODUCTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("EVT-1001"));
    }

    @Test
    void getWithInvalidTypeShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/events").param("type", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    @Test
    void getUnknownEventShouldReturn404() throws Exception {
        when(eventService.findByEventId("NOPE")).thenThrow(new EventNotFoundException("NOPE"));

        mockMvc.perform(get("/api/events/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
