package com.condowhats.adapter.in.web.dto.response;

import com.condowhats.domain.model.Occurrence;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados de uma ocorrência")
public record OccurrenceResponse(
        @Schema(example = "7") Long id,
        @Schema(example = "OC-2025-00007") String ticketNumber,
        @Schema(example = "Barulho no 3° andar") String title,
        @Schema(example = "Festa até às 3h da manhã com música alta") String description,
        @Schema(example = "NOISE") String category,
        @Schema(example = "OPEN") String status,
        @Schema(example = "MEDIUM") String priority,
        @Schema(example = "João da Silva") String residentName,
        @Schema(example = "204") String unitNumber,
        Instant createdAt,
        Instant resolvedAt
) {
    public static OccurrenceResponse from(Occurrence o) {
        return new OccurrenceResponse(
                o.getId(), o.getTicketNumber(), o.getTitle(), o.getDescription(),
                o.getCategory().name(), o.getStatus().name(), o.getPriority().name(),
                o.getResident().getName(), o.getResident().getUnitNumber(),
                o.getCreatedAt(), o.getResolvedAt()
        );
    }
}
