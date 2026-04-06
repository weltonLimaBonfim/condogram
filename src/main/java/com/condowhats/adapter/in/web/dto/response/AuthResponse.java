package com.condowhats.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens de acesso retornados após autenticação")
public record AuthResponse(
        @Schema(description = "Bearer token para usar nos demais endpoints", example = "eyJhbGci...")
        String accessToken,

        @Schema(description = "Token para renovar o access token sem novo login", example = "eyJhbGci...")
        String refreshToken,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,

        @Schema(description = "Validade do access token em milissegundos", example = "3600000")
        long expiresIn,

        @Schema(description = "E-mail do usuário autenticado")
        String email,

        @Schema(description = "Papel do usuário", example = "SYNDIC")
        String role
) {
}
