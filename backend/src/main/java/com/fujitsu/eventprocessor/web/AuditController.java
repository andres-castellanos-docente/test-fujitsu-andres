package com.fujitsu.eventprocessor.web;

import com.fujitsu.eventprocessor.dto.AuditLogResponse;
import com.fujitsu.eventprocessor.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audits")
@Tag(name = "Auditoria", description = "Consulta dos registros de auditoria")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista os registros de auditoria (mais recentes primeiro)")
    public List<AuditLogResponse> list() {
        return auditService.findAll();
    }
}
