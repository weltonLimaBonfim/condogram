package com.condowhats.domain.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationCreated(
        Long reservationId,
        Long residentId,
        Long condominiumId,
        String areaName,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
