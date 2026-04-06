package com.condowhats.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro de um morador")
public record CreateResidentRequest(

        @Schema(example = "João da Silva") @NotBlank @Size(max = 150)
        String name,

        @Schema(description = "CPF somente dígitos (11 números)", example = "12345678901")
        @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos")
        String cpf,

        @Schema(example = "204") @NotBlank @Size(max = 20)
        String unitNumber,

        @Schema(example = "B")
        String block,

        @Schema(example = "RESIDENT", allowableValues = {"RESIDENT", "OWNER", "SYNDIC", "ADMIN"})
        String role
) {
}
