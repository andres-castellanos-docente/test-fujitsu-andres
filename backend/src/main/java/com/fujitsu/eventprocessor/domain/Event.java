package com.fujitsu.eventprocessor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Evento recebido de um equipamento externo.
 * <p>
 * A restrição única em {@code event_id} é a garantia final contra duplicidade,
 * inclusive em cenários de requisições concorrentes.
 */
@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(name = "uk_events_event_id", columnNames = "event_id"))
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 50)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(nullable = false, length = 500)
    private String description;

    /** Data/hora em que o evento ocorreu no equipamento. */
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime timestamp;

    /** Data/hora em que o evento foi recebido pela API. */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected Event() {
        // exigido pelo JPA
    }

    public Event(String eventId, EventType eventType, String description, LocalDateTime timestamp, Instant receivedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.description = description;
        this.timestamp = timestamp;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
