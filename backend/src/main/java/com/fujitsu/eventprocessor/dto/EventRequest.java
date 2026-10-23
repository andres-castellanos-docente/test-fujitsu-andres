package com.fujitsu.eventprocessor.dto;

import com.fujitsu.eventprocessor.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Payload de entrada de um evento enviado por um equipamento.
 */
public record EventRequest(

        @Schema(example = "EVT-1001")
        @NotBlank(message = "O eventId é obrigatório")
        @Size(max = 50, message = "O eventId deve ter no máximo 50 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "O eventId deve conter apenas letras, números, '-' ou '_'")
        String eventId,

        @Schema(example = "PRODUCTION")
        @NotNull(message = "O eventType é obrigatório")
        EventType eventType,

        @Schema(example = "Production line started")
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String description,

        @Schema(example = "2026-05-20T10:00:00")
        @NotNull(message = "O timestamp é obrigatório")
        LocalDateTime timestamp
) {
}
