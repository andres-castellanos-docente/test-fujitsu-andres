package com.fujitsu.eventprocessor.dto;

import com.fujitsu.eventprocessor.domain.AuditLog;
import com.fujitsu.eventprocessor.domain.EventType;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String eventId,
        EventType eventType,
        String action,
        String details,
        Instant createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEventId(),
                auditLog.getEventType(),
                auditLog.getAction(),
                auditLog.getDetails(),
                auditLog.getCreatedAt());
    }
}
