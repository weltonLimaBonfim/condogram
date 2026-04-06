package com.condowhats.domain.event;

import com.condowhats.domain.model.Occurrence.OccurrenceCategory;

public record OccurrenceCreationRequested(
        Long condominiumId,
        Long residentId,
        Long sessionId,
        Long originEventId,
        String title,
        String description,
        OccurrenceCategory category
) {
}
