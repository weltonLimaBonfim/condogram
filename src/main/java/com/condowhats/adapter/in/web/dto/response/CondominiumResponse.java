package com.condowhats.adapter.in.web.dto.response;

import com.condowhats.domain.model.Condominium;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados de um condomínio")
public record CondominiumResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Residencial das Flores") String name,
        @Schema(example = "São Paulo") String city,
        @Schema(example = "SP") String state,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "2025-01-15T10:00:00Z") Instant createdAt
) {
    public static CondominiumResponse from(Condominium c) {
        return new CondominiumResponse(
                c.getId(), c.getName(), c.getCity(), c.getState(),
                c.getStatus().name(), c.getCreatedAt()
        );
    }
}
