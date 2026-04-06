package com.condowhats.domain.repository;

import com.condowhats.domain.model.CommonArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonAreaRepository extends JpaRepository<CommonArea, Long> {
    List<CommonArea> findByCondominiumIdAndActiveTrue(Long condominiumId);
}
