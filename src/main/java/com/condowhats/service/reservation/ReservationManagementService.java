package com.condowhats.service.reservation;

import com.condowhats.adapter.in.web.dto.response.PageResponse;
import com.condowhats.adapter.in.web.dto.response.ReservationResponse;
import com.condowhats.domain.model.Reservation.ReservationStatus;
import com.condowhats.domain.repository.ReservationRepository;
import com.condowhats.exception.BusinessRuleException;
import com.condowhats.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationManagementService {

    private static final Set<ReservationStatus> CANCELLABLE_STATUSES =
            Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepo;

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> list(Long condoId, LocalDate date, int page, int size) {
        if (date != null) {
            List<ReservationResponse> items = reservationRepo
                    .findByCondominiumIdAndReservationDate(condoId, date).stream()
                    .map(ReservationResponse::from)
                    .toList();
            return new PageResponse<>(items, 0, items.size(), items.size(), 1, true);
        }

        var pageable = PageRequest.of(page, size, Sort.by("reservationDate").descending());
        return PageResponse.from(
                reservationRepo.findByCondominiumId(condoId, pageable).map(ReservationResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(Long condoId, Long id) {
        return reservationRepo.findById(id)
                .filter(r -> r.getCondominium().getId().equals(condoId))
                .map(ReservationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
    }

    @Transactional
    public ReservationResponse cancel(Long condoId, Long id, String reason) {
        var reservation = reservationRepo.findById(id)
                .filter(r -> r.getCondominium().getId().equals(condoId))
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", id));

        if (!CANCELLABLE_STATUSES.contains(reservation.getStatus())) {
            throw new BusinessRuleException(
                    "Não é possível cancelar uma reserva com status " + reservation.getStatus());
        }

        if (reservation.getReservationDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Não é possível cancelar uma reserva passada");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledReason(reason);
        reservation.setCancelledAt(Instant.now());

        log.info("Reserva cancelada: id={} condo={} motivo={}", id, condoId, reason);
        return ReservationResponse.from(reservationRepo.save(reservation));
    }
}
