package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.UnitOfMeasureCategory;
import com.rayvision.inventory_management.service.UnitOfMeasureCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/unit-of-measures/categories")
public class UnitOfMeasureCategoryController {

    private final UnitOfMeasureCategoryService categoryService;

    @Autowired
    public UnitOfMeasureCategoryController(UnitOfMeasureCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * GET all UOM categories for a given company.
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<UnitOfMeasureCategory>> getAllCategories(@PathVariable Long companyId) {
        List<UnitOfMeasureCategory> categories = categoryService.getAll(companyId);
        return ResponseEntity.ok(categories);
    }

    /**
     * GET a single UOM category by ID for a given company.
     */
    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<UnitOfMeasureCategory> getCategoryById(@PathVariable Long id, @PathVariable Long companyId) {
        Optional<UnitOfMeasureCategory> categoryOpt = categoryService.findById(id);
        return categoryOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST a new UOM category.
     */
    @PostMapping("/company/{companyId}")
    public ResponseEntity<UnitOfMeasureCategory> createCategory(@PathVariable Long companyId,
                                                                @RequestBody UnitOfMeasureCategory category) {
        UnitOfMeasureCategory saved = categoryService.save(companyId, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PATCH to partially update an existing UOM category.
     */
    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<UnitOfMeasureCategory> partialUpdateCategory(@PathVariable Long id,
                                                                       @PathVariable Long companyId,
                                                                       @RequestBody UnitOfMeasureCategory update) {
        Optional<UnitOfMeasureCategory> categoryOpt = categoryService.findById(id);
        if (categoryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        UnitOfMeasureCategory existing = categoryOpt.get();
        if (update.getName() != null) {
            existing.setName(update.getName());
        }
        if (update.getDescription() != null) {
            existing.setDescription(update.getDescription());
        }
        UnitOfMeasureCategory updated = categoryService.save(companyId, existing);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE a UOM category by ID.
     */
    @DeleteMapping("/{id}/company/{companyId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, @PathVariable Long companyId) {
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
