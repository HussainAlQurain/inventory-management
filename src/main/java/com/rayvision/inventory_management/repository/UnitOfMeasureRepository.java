package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    List<UnitOfMeasure> findByCompanyId(Long companyId);
    Optional<UnitOfMeasure> findByCompanyIdAndId(Long companyId, Long id);
}
