package com.condowhats.service.reservation;

import com.condowhats.domain.event.ReservationConflicted;
import com.condowhats.domain.event.ReservationCreated;
import com.condowhats.domain.event.ReservationRequested;
import com.condowhats.domain.model.CommonArea;
import com.condowhats.domain.model.Condominium;
import com.condowhats.domain.model.Reservation;
import com.condowhats.domain.model.Resident;
import com.condowhats.domain.repository.CommonAreaRepository;
import com.condowhats.domain.repository.CondominiumRepository;
import com.condowhats.domain.repository.ReservationRepository;
import com.condowhats.domain.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final ApplicationEventPublisher eventBus;
    private final ReservationRepository reservationRepo;
    private final CommonAreaRepository areaRepo;
    private final ResidentRepository residentRepo;
    private final CondominiumRepository condoRepo;

    @Async
    @EventListener
    @Transactional
    public void onReservationRequested(ReservationRequested event) {
        CommonArea area = areaRepo.findById(event.commonAreaId()).orElseThrow();
        Resident resident = residentRepo.findById(event.residentId()).orElseThrow();
        Condominium condo = condoRepo.findById(event.condominiumId()).orElseThrow();

        // Verifica conflito de horário
        List<Reservation> conflicts = reservationRepo.findConflicting(
                area.getId(), event.reservationDate(), event.startTime(), event.endTime()
        );

        if (!conflicts.isEmpty()) {
            log.warn("Conflito de reserva: area={} data={}", area.getId(), event.reservationDate());
            // Publica evento de conflito para o bot notificar o morador
            eventBus.publishEvent(new ReservationConflicted(
                    event.residentId(), event.condominiumId(),
                    area.getName(), event.reservationDate().format(DATE_FMT)
            ));
            return;
        }

        Reservation reservation = reservationRepo.save(Reservation.builder()
                .commonArea(area)
                .condominium(condo)
                .resident(resident)
                .reservationDate(event.reservationDate())
                .startTime(event.startTime())
                .endTime(event.endTime())
                .status(area.getRequiresApproval()
                        ? Reservation.ReservationStatus.PENDING
                        : Reservation.ReservationStatus.CONFIRMED)
                .build());

        log.info("Reserva criada | id={} area={} data={}", reservation.getId(), area.getName(), event.reservationDate());

        eventBus.publishEvent(new ReservationCreated(
                reservation.getId(),
                resident.getId(),
                condo.getId(),
                area.getName(),
                event.reservationDate(),
                event.startTime(),
                event.endTime()
        ));
    }

    /**
     * Utilitário: faz parse de "DD/MM/AAAA" → LocalDate
     */
    public Optional<LocalDate> parseDate(String input) {
        try {
            return Optional.of(LocalDate.parse(input.trim(), DATE_FMT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * Utilitário: faz parse de "HH:mm às HH:mm" ou "HH:mm-HH:mm" → [start, end]
     */
    public Optional<LocalTime[]> parseTimeRange(String input) {
        try {
            String cleaned = input.replaceAll("(?i)\\s*às\\s*", "-").replace(" ", "");
            String[] parts = cleaned.split("-");
            if (parts.length != 2) return Optional.empty();
            return Optional.of(new LocalTime[]{
                    LocalTime.parse(parts[0].trim(), TIME_FMT),
                    LocalTime.parse(parts[1].trim(), TIME_FMT)
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
