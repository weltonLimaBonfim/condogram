package com.condowhats.domain.repository;

import com.condowhats.domain.model.InteractionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionEventRepository extends JpaRepository<InteractionEvent, Long> {
    Page<InteractionEvent> findByCondominiumIdOrderByOccurredAtDesc(Long condominiumId, Pageable pageable);

    Page<InteractionEvent> findByResidentIdOrderByOccurredAtDesc(Long residentId, Pageable pageable);
}
