package com.condowhats.domain.repository;

import com.condowhats.domain.model.Resident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    Page<Resident> findByCondominiumId(Long condominiumId, Pageable pageable);

    Page<Resident> findByCondominiumIdAndStatus(Long condominiumId, Resident.ResidentStatus status, Pageable pageable);

    Optional<Resident> findByIdAndCondominiumId(Long id, Long condominiumId);

    /**
     * Busca por CPF dentro de um condomínio específico
     */
    Optional<Resident> findByCondominiumIdAndCpf(Long condominiumId, String cpf);

    /**
     * Busca por CPF em todos os condomínios — usado pelo bot único.
     * Retorna lista porque um CPF pode estar em múltiplos condomínios.
     */
    @Query("SELECT r FROM Resident r WHERE r.cpf = :cpf AND r.status = 'ACTIVE' ORDER BY r.condominium.name")
    List<Resident> findAllByCpfAndActive(String cpf);
}
