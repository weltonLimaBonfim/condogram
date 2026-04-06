package com.condowhats.domain.repository;

import com.condowhats.domain.model.ManagementCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagementCompanyRepository extends JpaRepository<ManagementCompany, Long> {
    Optional<ManagementCompany> findByCnpj(String cnpj);
}
