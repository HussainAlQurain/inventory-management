package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.repository.CategoryRepository;
import com.rayvision.inventory_management.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    CategoryRepository categoryRepository;
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> getAllCategories(Long companyId) {
        // Later we can adjust category to be for each company.
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> getCategoryById(Long companyId, Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public Category save(Long companyId, Category category) {
        return null;
    }

    @Override
    public Category update(Long companyId, Category category) {
        return null;
    }

    @Override
    public Category partialUpdate(Long companyId, Category category) {
        return null;
    }

    @Override
    public void deleteCategoryById(Long companyId, Long id) {

    }
}
