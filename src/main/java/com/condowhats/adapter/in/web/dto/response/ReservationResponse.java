package com.condowhats.adapter.in.web.dto.response;

import com.condowhats.domain.model.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Dados de uma reserva")
public record ReservationResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "Salão de Festas") String areaName,
        @Schema(example = "João da Silva") String residentName,
        @Schema(example = "204") String unitNumber,
        @Schema(example = "2025-06-20") LocalDate reservationDate,
        @Schema(example = "14:00") LocalTime startTime,
        @Schema(example = "18:00") LocalTime endTime,
        @Schema(example = "CONFIRMED") String status,
        Instant createdAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getCommonArea().getName(),
                r.getResident().getName(), r.getResident().getUnitNumber(),
                r.getReservationDate(), r.getStartTime(), r.getEndTime(),
                r.getStatus().name(), r.getCreatedAt()
        );
    }
}
