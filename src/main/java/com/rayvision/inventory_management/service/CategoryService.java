package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.model.dto.FilterOptionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> getAllCategories(Long companyId);
    Optional<Category> getCategoryById(Long companyId, Long id);
    Category save(Long companyId, Category category);
    Category update(Long companyId, Category category);
    Category partialUpdate(Long companyId, Category category);
    void deleteCategoryById(Long companyId, Long id);
    List<Category> searchForInventoryItemsOrUnused(Long companyId, String search);
    List<FilterOptionDTO> findFilterOptions(Long companyId, String search);
    Page<FilterOptionDTO> findPaginatedFilterOptions(Long companyId, String search, Pageable pageable);
}
