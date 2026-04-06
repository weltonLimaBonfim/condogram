package com.condowhats.domain.repository;

import com.condowhats.domain.model.Occurrence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OccurrenceRepository extends JpaRepository<Occurrence, Long> {
    Page<Occurrence> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(o.ticketNumber, 9) AS int)), 0) FROM Occurrence o WHERE o.condominium.id = :condoId AND YEAR(o.createdAt) = :year")
    int findLastTicketSequence(Long condoId, int year);

    Optional<Occurrence> findByCondominiumIdAndTicketNumber(Long condoId, String ticket);
}
