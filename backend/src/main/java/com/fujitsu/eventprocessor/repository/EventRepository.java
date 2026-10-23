package com.fujitsu.eventprocessor.repository;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByEventId(String eventId);

    Optional<Event> findByEventId(String eventId);

    List<Event> findAllByOrderByTimestampDesc();

    List<Event> findByEventTypeOrderByTimestampDesc(EventType eventType);
}
