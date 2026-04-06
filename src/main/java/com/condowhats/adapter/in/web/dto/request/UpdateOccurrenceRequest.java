package com.condowhats.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para atualizar uma ocorrência")
public record UpdateOccurrenceRequest(

        @Schema(description = "Novo status", example = "IN_PROGRESS",
                allowableValues = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "CANCELLED"})
        @NotNull
        String status,

        @Schema(description = "Nota ou observação sobre a atualização", example = "Acionado técnico para verificar o vazamento")
        @NotBlank
        String note,

        @Schema(description = "Nome de quem está atualizando", example = "Síndico Carlos")
        @NotBlank
        String updatedBy,

        @Schema(description = "Se true, notifica o morador via Telegram", example = "true")
        Boolean notifyResident
) {
}
