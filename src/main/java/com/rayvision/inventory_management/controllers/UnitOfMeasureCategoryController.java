package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.UnitOfMeasureCategoryMapper;
import com.rayvision.inventory_management.model.UnitOfMeasureCategory;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCategoryCreateDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCategoryResponseDTO;
import com.rayvision.inventory_management.service.UnitOfMeasureCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/unit-of-measures/categories")
public class UnitOfMeasureCategoryController {

    private final UnitOfMeasureCategoryService categoryService;
    private final UnitOfMeasureCategoryMapper unitOfMeasureCategoryMapper;

    @Autowired
    public UnitOfMeasureCategoryController(UnitOfMeasureCategoryService categoryService, UnitOfMeasureCategoryMapper unitOfMeasureCategoryMapper) {
        this.categoryService = categoryService;
        this.unitOfMeasureCategoryMapper = unitOfMeasureCategoryMapper;
    }

    /**
     * GET all UOM categories for a given company.
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<UnitOfMeasureCategoryResponseDTO>> getAllCategories(@PathVariable Long companyId) {
        List<UnitOfMeasureCategory> categories = categoryService.getAll(companyId);
        List<UnitOfMeasureCategoryResponseDTO> dtos = categories.stream()
                .map(unitOfMeasureCategoryMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET a single UOM category by ID for a given company.
     */
    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<UnitOfMeasureCategoryResponseDTO> getCategoryById(@PathVariable Long id, @PathVariable Long companyId) {
        Optional<UnitOfMeasureCategory> categoryOpt = categoryService.findById(id);
        return categoryOpt.map(category -> ResponseEntity.ok(unitOfMeasureCategoryMapper.toResponseDTO(category)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST a new UOM category.
     */
    @PostMapping("/company/{companyId}")
    public ResponseEntity<UnitOfMeasureCategoryResponseDTO> createCategory(@PathVariable Long companyId,
                                                                           @RequestBody UnitOfMeasureCategoryCreateDTO createDto) {
        UnitOfMeasureCategory category = unitOfMeasureCategoryMapper.fromCreateDTO(createDto);
        UnitOfMeasureCategory saved = categoryService.save(companyId, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(unitOfMeasureCategoryMapper.toResponseDTO(saved));
    }

    /**
     * PATCH to partially update an existing UOM category.
     */
    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<UnitOfMeasureCategoryResponseDTO> partialUpdateCategory(@PathVariable Long id,
                                                                                  @PathVariable Long companyId,
                                                                                  @RequestBody UnitOfMeasureCategoryCreateDTO dto) {
        Optional<UnitOfMeasureCategory> categoryOpt = categoryService.findById(id);
        if (categoryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        UnitOfMeasureCategory existing = categoryOpt.get();
        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        UnitOfMeasureCategory updated = categoryService.save(companyId, existing);
        return ResponseEntity.ok(unitOfMeasureCategoryMapper.toResponseDTO(updated));
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
