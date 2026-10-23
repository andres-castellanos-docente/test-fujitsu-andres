package com.fujitsu.eventprocessor.service;

import com.fujitsu.eventprocessor.domain.AuditLog;
import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.dto.AuditLogResponse;
import com.fujitsu.eventprocessor.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuditService {

    public static final String ACTION_QUALITY_EVENT_REGISTERED = "QUALITY_EVENT_REGISTERED";

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final DateTimeFormatter DETAILS_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditService(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    /**
     * Gera o registro de auditoria de um evento QUALITY.
     * Participa da mesma transação do registro do evento: ou ambos são gravados, ou nenhum.
     */
    @Transactional
    public AuditLog registerQualityEvent(Event event) {
        String details = "Evento de qualidade registrado. Ocorrido em %s. Descrição: %s"
                .formatted(DETAILS_DATE_FORMAT.format(event.getTimestamp()), event.getDescription());

        AuditLog auditLog = auditLogRepository.save(new AuditLog(
                event.getEventId(),
                event.getEventType(),
                ACTION_QUALITY_EVENT_REGISTERED,
                details,
                Instant.now(clock)));

        log.info("Auditoria gerada para o evento {} (auditId={})", event.getEventId(), auditLog.getId());
        return auditLog;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findAll() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
