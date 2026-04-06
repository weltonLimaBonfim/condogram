package com.condowhats.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para envio de comunicado para todos os moradores")
public record SendAnnouncementRequest(

        @Schema(description = "Assunto do comunicado", example = "Manutenção do elevador")
        @NotBlank @Size(max = 150)
        String subject,

        @Schema(description = "Texto completo do comunicado", example = "Informamos que o elevador do bloco A estará em manutenção no dia 20/06.")
        @NotBlank @Size(max = 1000)
        String body
) {
}
