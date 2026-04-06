package com.condowhats.adapter.in.web.dto.response;

import com.condowhats.domain.model.AppUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados de um usuário do sistema")
public record UserResponse(
        @Schema(example = "5") Long id,
        @Schema(example = "carlos@residencialflores.com.br") String email,
        @Schema(example = "Carlos Silva") String fullName,
        @Schema(example = "SYNDIC") String role,
        @Schema(example = "1") Long condominiumId,
        @Schema(example = "true") Boolean active,
        Instant createdAt
) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getFullName(),
                u.getRole().name(),
                u.getCondominium() != null ? u.getCondominium().getId() : null,
                u.getActive(), u.getCreatedAt()
        );
    }
}
