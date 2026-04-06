package com.condowhats.service.occurrence;

import com.condowhats.domain.event.OccurrenceCreated;
import com.condowhats.domain.event.OccurrenceCreationRequested;
import com.condowhats.domain.model.Condominium;
import com.condowhats.domain.model.Occurrence;
import com.condowhats.domain.model.Resident;
import com.condowhats.domain.repository.CondominiumRepository;
import com.condowhats.domain.repository.OccurrenceRepository;
import com.condowhats.domain.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OccurrenceService {

    private final ApplicationEventPublisher eventBus;
    private final OccurrenceRepository occurrenceRepo;
    private final ResidentRepository residentRepo;
    private final CondominiumRepository condoRepo;

    @Async
    @EventListener
    @Transactional
    public void onOccurrenceRequested(OccurrenceCreationRequested event) {
        Resident resident = residentRepo.findById(event.residentId()).orElseThrow();
        Condominium condo = condoRepo.findById(event.condominiumId()).orElseThrow();

        String ticket = generateTicket(condo.getId());

        Occurrence occurrence = occurrenceRepo.save(Occurrence.builder()
                .condominium(condo)
                .resident(resident)
                .ticketNumber(ticket)
                .title(event.title())
                .description(event.description())
                .category(event.category())
                .status(Occurrence.OccurrenceStatus.OPEN)
                .priority(Occurrence.Priority.MEDIUM)
                .build());

        log.info("Ocorrência criada | ticket={} condo={}", ticket, condo.getId());

        eventBus.publishEvent(new OccurrenceCreated(
                occurrence.getId(), ticket,
                resident.getId(), condo.getId()
        ));
    }

    private String generateTicket(Long condoId) {
        int year = LocalDate.now().getYear();
        int seq = occurrenceRepo.findLastTicketSequence(condoId, year) + 1;
        return String.format("OC-%d-%05d", year, seq);
    }
}
