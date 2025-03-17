package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.UnitOfMeasureCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitOfMeasureCategoryRepository extends JpaRepository<UnitOfMeasureCategory, Long> {
    List<UnitOfMeasureCategory> findByCompanyId(Long companyId);
}
