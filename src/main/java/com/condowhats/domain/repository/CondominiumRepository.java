package com.condowhats.domain.repository;

import com.condowhats.domain.model.Condominium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominiumRepository extends JpaRepository<Condominium, Long> {
}
