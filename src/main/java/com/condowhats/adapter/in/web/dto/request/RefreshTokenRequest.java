package com.condowhats.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token para renovar o access token")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token obtido no login")
        @NotBlank
        String refreshToken
) {
}
