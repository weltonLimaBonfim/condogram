package com.condowhats.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro de um novo condomínio")
public record CreateCondominiumRequest(

        @Schema(example = "Residencial das Flores") @NotBlank @Size(max = 150)
        String name,

        @Schema(example = "12.345.678/0001-99")
        String cnpj,

        @Schema(example = "Rua das Flores, 100") @NotBlank @Size(max = 255)
        String address,

        @Schema(example = "São Paulo") @NotBlank @Size(max = 100)
        String city,

        @Schema(example = "SP") @NotBlank @Size(min = 2, max = 2)
        String state,

        @Schema(example = "01310-100")
        String zipCode,

        @Schema(example = "Olá! Bem-vindo ao Residencial das Flores.")
        String botGreeting,

        @Schema(description = "ID da administradora", example = "1")
        Long managementCompanyId
) {
}
