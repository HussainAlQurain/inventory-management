package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.UnitOfMeasureCategory;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.UnitOfMeasureCategoryRepository;
import com.rayvision.inventory_management.service.UnitOfMeasureCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UnitOfMeasureCategoryServiceImpl implements UnitOfMeasureCategoryService {
    private final UnitOfMeasureCategoryRepository repository;
    private final CompanyRepository companyRepository;

    public UnitOfMeasureCategoryServiceImpl(UnitOfMeasureCategoryRepository repository, CompanyRepository companyRepository) {
        this.repository = repository;
        this.companyRepository = companyRepository;
    }

    @Override
    public UnitOfMeasureCategory save(Long companyId, UnitOfMeasureCategory category) {
        // If you need to associate the category with the company, do it here.
        // For example:
        category.setCompany(companyRepository.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found")));
        return repository.save(category);
    }

    @Override
    public Optional<UnitOfMeasureCategory> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<UnitOfMeasureCategory> getAll(Long companyId) {
        return repository.findByCompanyId(companyId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }


}
