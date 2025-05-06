package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.InventoryItemResponseMapper;
import com.rayvision.inventory_management.mappers.impl.CategoryMapper;
import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.model.dto.CategoryCreateDTO;
import com.rayvision.inventory_management.model.dto.CategoryPartialUpdateDTO;
import com.rayvision.inventory_management.model.dto.CategoryResponseDTO;
import com.rayvision.inventory_management.model.dto.FilterOptionDTO;
import com.rayvision.inventory_management.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final InventoryItemResponseMapper inventoryItemResponseMapper;

    @Autowired
    public CategoryController(CategoryService categoryService, InventoryItemResponseMapper inventoryItemResponseMapper) {
        this.categoryService = categoryService;
        this.inventoryItemResponseMapper = inventoryItemResponseMapper;
    }

    /**
     * GET all categories for a given company
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories(
            @PathVariable Long companyId,
            @RequestParam(name = "search", required = false, defaultValue = "") String searchTerm
    ) {
        List<Category> categories = categoryService.searchForInventoryItemsOrUnused(companyId, searchTerm);

        List<CategoryResponseDTO> responseList = categories.stream()
                .map(inventoryItemResponseMapper::toCategoryResponseDTO)
                .toList();

        return ResponseEntity.ok(responseList);
    }


    /**
     * GET a single category by ID
     */
    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(
            @PathVariable Long id,
            @PathVariable Long companyId
    ) {
        Optional<Category> catOpt = categoryService.getCategoryById(companyId, id);
        if (catOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CategoryResponseDTO dto = inventoryItemResponseMapper.toCategoryResponseDTO(catOpt.get());
        return ResponseEntity.ok(dto);
    }

    /**
     * POST a new category
     */
    @PostMapping("/company/{companyId}")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @PathVariable Long companyId,
            @RequestBody CategoryCreateDTO createDto
    ) {
        // 1) Build or map an entity from createDto
        //    If your service expects a Category entity, you can do manual or mapstruct mapping:
        Category newCat = new Category();
        newCat.setName(createDto.getName());
        newCat.setDescription(createDto.getDescription());
        // The service’s save(...) method will attach the company as well.

        // 2) Call service
        Category savedCat = categoryService.save(companyId, newCat);

        // 3) Return
        CategoryResponseDTO resp = inventoryItemResponseMapper.toCategoryResponseDTO(savedCat);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * PATCH to partially update an existing category
     */
    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<CategoryResponseDTO> partialUpdate(
            @PathVariable Long id,
            @PathVariable Long companyId,
            @RequestBody CategoryPartialUpdateDTO patchDto
    ) {
        // Build a Category entity with only the patch fields
        Category patchEntity = new Category();
        patchEntity.setId(id);
        // only set name/description if they’re not null from patchDto—**or** you do it in the service
        patchEntity.setName(patchDto.getName());
        patchEntity.setDescription(patchDto.getDescription());

        Category updated = categoryService.partialUpdate(companyId, patchEntity);
        CategoryResponseDTO resp = inventoryItemResponseMapper.toCategoryResponseDTO(updated);
        return ResponseEntity.ok(resp);
    }

    /**
     * DELETE a category by ID
     */
    @DeleteMapping("/{id}/company/{companyId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @PathVariable Long companyId
    ) {
        categoryService.deleteCategoryById(companyId, id);
        return ResponseEntity.noContent().build();
    }


    // Add to CategoryController
    @GetMapping("/company/{companyId}/filter-options")
    public ResponseEntity<List<FilterOptionDTO>> getCategoryFilterOptions(
            @PathVariable Long companyId,
            @RequestParam(required = false, defaultValue = "") String search) {

        List<FilterOptionDTO> options = categoryService.findFilterOptions(companyId, search);
        return ResponseEntity.ok(options);
    }
}
