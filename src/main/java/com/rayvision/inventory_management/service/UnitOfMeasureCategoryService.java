package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.UnitOfMeasureCategory;

import java.util.Optional;

public interface UnitOfMeasureCategoryService {
    /**
     * Saves a new UnitOfMeasureCategory.
     *
     * @param companyId the ID of the company (if needed)
     * @param category  the category to save
     * @return the persisted UnitOfMeasureCategory
     */
    UnitOfMeasureCategory save(Long companyId, UnitOfMeasureCategory category);

    /**
     * Finds a UnitOfMeasureCategory by its ID.
     *
     * @param id the category ID
     * @return an Optional containing the category if found, otherwise empty.
     */
    Optional<UnitOfMeasureCategory> findById(Long id);

}
