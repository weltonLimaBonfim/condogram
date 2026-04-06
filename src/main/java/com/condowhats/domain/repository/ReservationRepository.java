package com.condowhats.domain.repository;

import com.condowhats.domain.model.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
                SELECT r FROM Reservation r
                WHERE r.commonArea.id = :areaId
                  AND r.reservationDate = :date
                  AND r.status <> 'CANCELLED'
                  AND r.startTime < :endTime
                  AND r.endTime > :startTime
            """)
    List<Reservation> findConflicting(Long areaId, LocalDate date, LocalTime startTime, LocalTime endTime);

    List<Reservation> findByCondominiumIdAndReservationDate(Long condoId, LocalDate date);

    Page<Reservation> findByCondominiumId(Long condoId, Pageable pageable);
}
