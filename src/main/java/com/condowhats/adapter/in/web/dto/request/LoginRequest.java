package com.condowhats.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais para autenticação")
public record LoginRequest(
        @Schema(example = "sindico@residencialflores.com.br")
        @NotBlank @Email
        String email,

        @Schema(example = "Senha@123")
        @NotBlank
        String password
) {
}
