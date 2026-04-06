package com.condowhats.domain.event;

public record OccurrenceCreated(
        Long occurrenceId,
        String ticketNumber,
        Long residentId,
        Long condominiumId
) {
}
