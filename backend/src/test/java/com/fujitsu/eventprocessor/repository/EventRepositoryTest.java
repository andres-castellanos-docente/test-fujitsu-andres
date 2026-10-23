package com.fujitsu.eventprocessor.repository;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository repository;

    private Event event(String id, EventType type, String timestamp) {
        return new Event(id, type, "desc", LocalDateTime.parse(timestamp), Instant.now());
    }

    @Test
    void uniqueConstraintShouldPreventDuplicateEventId() {
        repository.saveAndFlush(event("EVT-1", EventType.PRODUCTION, "2026-05-20T10:00:00"));

        assertThatThrownBy(() -> repository.saveAndFlush(event("EVT-1", EventType.QUALITY, "2026-05-20T11:00:00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindByTypeOrderedByTimestampDesc() {
        repository.save(event("EVT-1", EventType.QUALITY, "2026-05-20T10:00:00"));
        repository.save(event("EVT-2", EventType.PRODUCTION, "2026-05-20T11:00:00"));
        repository.save(event("EVT-3", EventType.QUALITY, "2026-05-20T12:00:00"));

        assertThat(repository.findByEventTypeOrderByTimestampDesc(EventType.QUALITY))
                .extracting(Event::getEventId)
                .containsExactly("EVT-3", "EVT-1");
        assertThat(repository.existsByEventId("EVT-2")).isTrue();
        assertThat(repository.existsByEventId("EVT-9")).isFalse();
    }
}
