package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.model.dto.FilterOptionDTO;
import com.rayvision.inventory_management.repository.CategoryRepository;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    CategoryRepository categoryRepository;
    CompanyRepository companyRepository;
    public CategoryServiceImpl(CategoryRepository categoryRepository, CompanyRepository companyRepository) {
        this.categoryRepository = categoryRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<Category> getAllCategories(Long companyId) {
        // Later we can adjust category to be for each company.
        return categoryRepository.findByCompanyId(companyId);
    }

    @Override
    public Optional<Category> getCategoryById(Long companyId, Long id) {
        return categoryRepository.findByCompanyIdAndId(companyId, id);
    }

    @Override
    public Category save(Long companyId, Category category) {
        return companyRepository.findById(companyId).map(
                company -> {
                    category.setCompany(company);
                    return categoryRepository.save(category);
                }).orElseThrow(() -> new RuntimeException("Invalid Company ID: " + companyId));
    }

    @Override
    public Category update(Long companyId, Category category) {
        return categoryRepository.findByCompanyIdAndId(companyId, category.getId())
                .map(existing -> {
                    existing.setName(category.getName());
                    existing.setDescription(category.getDescription());
                    // Update other fields if needed
                    return categoryRepository.save(existing);
                }).orElseThrow(() -> new RuntimeException("Category not found for company: " + companyId));

    }

    @Override
    public Category partialUpdate(Long companyId, Category category) {
        return categoryRepository.findByCompanyIdAndId(companyId, category.getId()).map(existingCategory -> {
            Optional.ofNullable(category.getName()).ifPresent(existingCategory::setName);
            Optional.ofNullable(category.getDescription()).ifPresent(existingCategory::setDescription);
            // If there are additional fields, add them here.
            return categoryRepository.save(existingCategory);
        }).orElseThrow(() -> new RuntimeException("Could not find category with id: " + category.getId()));
    }

    @Override
    public void deleteCategoryById(Long companyId, Long id) {
        Category category = categoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new RuntimeException("Category not found for company: " + companyId));
        categoryRepository.delete(category);
    }

    public List<Category> searchForInventoryItemsOrUnused(Long companyId, String search) {
        // If we want to treat null as empty, do:
        if (search == null) search = "";
        return categoryRepository.searchForInventoryItemsOrUnused(companyId, search);
    }

    // Add to CategoryServiceImpl
    @Override
    public List<FilterOptionDTO> findFilterOptions(Long companyId, String search) {
        if (search == null) search = "";
        return categoryRepository.findFilterOptions(companyId, search);
    }

}
