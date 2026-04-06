package com.condowhats.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar um usuário do sistema (síndico ou admin)")
public record CreateUserRequest(
        @Schema(example = "Carlos Silva")
        @NotBlank @Size(max = 100)
        String fullName,

        @Schema(example = "carlos@residencialflores.com.br")
        @NotBlank @Email
        String email,

        @Schema(example = "Senha@Segura123", description = "Mínimo 8 caracteres")
        @NotBlank @Size(min = 8)
        String password,

        @Schema(example = "SYNDIC", allowableValues = {"PLATFORM_ADMIN", "MANAGER", "SYNDIC"})
        String role,

        @Schema(example = "1", description = "ID do condomínio (obrigatório para SYNDIC)")
        Long condominiumId
) {
}
