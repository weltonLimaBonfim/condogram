package com.condowhats.adapter.in.web.dto.response;

import com.condowhats.domain.model.Resident;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados de um morador")
public record ResidentResponse(
        @Schema(example = "42") Long id,
        @Schema(example = "João da Silva") String name,
        @Schema(example = "***456789**") String cpfMasked,
        @Schema(example = "204") String unitNumber,
        @Schema(example = "B") String block,
        @Schema(example = "RESIDENT") String role,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "2025-01-15T10:00:00Z") Instant createdAt
) {
    public static ResidentResponse from(Resident r) {
        return new ResidentResponse(
                r.getId(), r.getName(),
                maskCpf(r.getCpf()),
                r.getUnitNumber(), r.getBlock(),
                r.getRole().name(), r.getStatus().name(), r.getCreatedAt()
        );
    }

    private static String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return "***.***.***-**";
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
