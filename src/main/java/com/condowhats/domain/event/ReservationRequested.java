package com.condowhats.domain.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationRequested(
        Long condominiumId,
        Long residentId,
        Long sessionId,
        Long originEventId,
        Long commonAreaId,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
