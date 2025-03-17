package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.UnitOfMeasureMapper;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureCreateDTO;
import com.rayvision.inventory_management.model.dto.UnitOfMeasureResponseDTO;
import com.rayvision.inventory_management.service.UnitOfMeasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UnitOfMeasureController {

    private final UnitOfMeasureService uomService;
    private final UnitOfMeasureMapper uomMapper;

    public UnitOfMeasureController(UnitOfMeasureService uomService, UnitOfMeasureMapper uomMapper) {
        this.uomService = uomService;
        this.uomMapper = uomMapper;
    }

    /**
     * GET all UnitOfMeasures for a given company.
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<UnitOfMeasureResponseDTO>> getAllUoms(@PathVariable Long companyId) {
        List<UnitOfMeasure> uoms = uomService.getAllUnitOfMeasures(companyId);
        List<UnitOfMeasureResponseDTO> dtos = uoms.stream()
                .map(uomMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET a single UnitOfMeasure by ID for a given company.
     */
    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<UnitOfMeasureResponseDTO> getUomById(@PathVariable Long id, @PathVariable Long companyId) {
        Optional<UnitOfMeasure> uomOpt = uomService.getById(companyId, id);
        return uomOpt.map(uom -> ResponseEntity.ok(uomMapper.toResponseDTO(uom)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST a new UnitOfMeasure.
     */
    @PostMapping("/company/{companyId}")
    public ResponseEntity<UnitOfMeasureResponseDTO> createUom(@PathVariable Long companyId,
                                                              @RequestBody UnitOfMeasureCreateDTO dto) {
        // Map from DTO to entity manually (or using a mapper if you prefer)
        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setName(dto.getName());
        uom.setAbbreviation(dto.getAbbreviation());
        uom.setConversionFactor(dto.getConversionFactor());
        // For the category, you could either set the category by its ID (if provided)
        // or if the details for a new category are provided, you might call your category service.
        // Here we assume you already have a valid category set on the DTO.
        if (dto.getCategoryId() != null) {
            // Here you would fetch the category entity and set it.
            // For example: uom.setCategory(categoryService.getById(dto.getCategoryId()).orElse(null));
        } else if (dto.getCategory() != null) {
            // Optionally, create a new category and then set it.
            // For simplicity, this example does not cover that.
        }

        UnitOfMeasure saved = uomService.save(companyId, uom);
        return ResponseEntity.status(HttpStatus.CREATED).body(uomMapper.toResponseDTO(saved));
    }

    /**
     * PATCH to partially update an existing UnitOfMeasure.
     */
    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<UnitOfMeasureResponseDTO> partialUpdateUom(@PathVariable Long id,
                                                                     @PathVariable Long companyId,
                                                                     @RequestBody UnitOfMeasureCreateDTO dto) {
        // Here we assume that the same DTO is used for creation and updates.
        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setId(id);
        if (dto.getName() != null) uom.setName(dto.getName());
        if (dto.getAbbreviation() != null) uom.setAbbreviation(dto.getAbbreviation());
        if (dto.getConversionFactor() != null) uom.setConversionFactor(dto.getConversionFactor());
        // Similarly, update the category if provided.
        UnitOfMeasure updated = uomService.partialUpdate(companyId, uom);
        return ResponseEntity.ok(uomMapper.toResponseDTO(updated));
    }

    /**
     * DELETE a UnitOfMeasure by ID.
     */
    @DeleteMapping("/{id}/company/{companyId}")
    public ResponseEntity<Void> deleteUom(@PathVariable Long id, @PathVariable Long companyId) {
        uomService.deleteUnitOfMeasureById(companyId, id);
        return ResponseEntity.noContent().build();
    }


}
