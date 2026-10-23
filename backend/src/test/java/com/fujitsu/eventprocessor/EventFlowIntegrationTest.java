package com.fujitsu.eventprocessor;

import com.fujitsu.eventprocessor.repository.AuditLogRepository;
import com.fujitsu.eventprocessor.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo (controller -> service -> handler -> H2), sem mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        eventRepository.deleteAll();
    }

    private static String body(String eventId, String type) {
        return """
                {"eventId":"%s","eventType":"%s","description":"Teste","timestamp":"2026-05-20T10:00:00"}
                """.formatted(eventId, type);
    }

    @Test
    void productionEventShouldBeRegisteredWithoutAudit() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body("EVT-P1", "PRODUCTION")))
                .andExpect(status().isCreated());

        assertThat(eventRepository.existsByEventId("EVT-P1")).isTrue();
        assertThat(auditLogRepository.findByEventId("EVT-P1")).isEmpty();
    }

    @Test
    void qualityEventShouldBeRegisteredWithAudit() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body("EVT-Q1", "QUALITY")))
                .andExpect(status().isCreated());

        assertThat(auditLogRepository.findByEventId("EVT-Q1")).hasSize(1);
        mockMvc.perform(get("/api/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("QUALITY_EVENT_REGISTERED"));
    }

    @Test
    void duplicateEventShouldReturn409AndNotGenerateSecondAudit() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body("EVT-D1", "QUALITY")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body("EVT-D1", "QUALITY")))
                .andExpect(status().isConflict());

        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.findByEventId("EVT-D1")).hasSize(1);
    }

    @Test
    void unknownRouteShouldReturn404InsteadOf500() throws Exception {
        mockMvc.perform(get("/api/rota-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
