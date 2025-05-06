package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.dto.UomFilterOptionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UnitOfMeasureService {
    List<UnitOfMeasure> getAllUnitOfMeasures(Long companyId);
    Optional<UnitOfMeasure> getById(Long companyId, Long id);
    UnitOfMeasure save(Long companyId, UnitOfMeasure unitOfMeasure);
    UnitOfMeasure update(Long companyId, UnitOfMeasure unitOfMeasure);
    UnitOfMeasure partialUpdate(Long companyId, UnitOfMeasure unitOfMeasure);
    void deleteUnitOfMeasureById(Long companyId, Long id);
    // Add this method to interface
    Page<UomFilterOptionDTO> findPaginatedFilterOptions(Long companyId, String search, Pageable pageable);
}
