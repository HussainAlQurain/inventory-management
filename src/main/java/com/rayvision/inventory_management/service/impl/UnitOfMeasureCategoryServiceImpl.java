package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.UnitOfMeasureCategory;
import com.rayvision.inventory_management.repository.UnitOfMeasureCategoryRepository;
import com.rayvision.inventory_management.service.UnitOfMeasureCategoryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UnitOfMeasureCategoryServiceImpl implements UnitOfMeasureCategoryService {
    private final UnitOfMeasureCategoryRepository repository;

    public UnitOfMeasureCategoryServiceImpl(UnitOfMeasureCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public UnitOfMeasureCategory save(Long companyId, UnitOfMeasureCategory category) {
        // If you need to associate the category with the company, do it here.
        // For example:
        // category.setCompany(companyService.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found")));
        return repository.save(category);
    }

    @Override
    public Optional<UnitOfMeasureCategory> findById(Long id) {
        return repository.findById(id);
    }

}
