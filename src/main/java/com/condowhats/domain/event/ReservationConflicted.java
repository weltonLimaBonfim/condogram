package com.condowhats.domain.event;

public record ReservationConflicted(
        Long residentId,
        Long condominiumId,
        String areaName,
        String reservationDate
) {
}
