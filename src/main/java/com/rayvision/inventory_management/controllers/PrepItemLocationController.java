package com.rayvision.inventory_management.controllers;


import com.rayvision.inventory_management.mappers.PrepItemLocationMapper;
import com.rayvision.inventory_management.model.PrepItemLocation;
import com.rayvision.inventory_management.model.dto.PrepItemLocationDTO;
import com.rayvision.inventory_management.service.PrepItemLocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tracks location-based on-hand for a PREPARATION subRecipe
 */

@RestController
@RequestMapping("/prep-locations")
public class PrepItemLocationController {
    private final PrepItemLocationService service;
    private final PrepItemLocationMapper mapper;

    public PrepItemLocationController(PrepItemLocationService service,
                                      PrepItemLocationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // GET bridging row by ID
    @GetMapping("/{id}")
    public ResponseEntity<PrepItemLocationDTO> getOne(@PathVariable Long id) {
        return service.getOne(id)
                .map(entity -> ResponseEntity.ok(mapper.toDto(entity)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CREATE bridging row
    @PostMapping
    public ResponseEntity<PrepItemLocationDTO> create(@RequestBody PrepItemLocationDTO dto) {
        PrepItemLocation created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(created));
    }

    // UPDATE bridging row
    @PatchMapping("/{id}")
    public ResponseEntity<PrepItemLocationDTO> update(@PathVariable Long id,
                                                      @RequestBody PrepItemLocationDTO dto) {
        try {
            PrepItemLocation updated = service.update(id, dto);
            return ResponseEntity.ok(mapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE bridging row
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET by subRecipe
    @GetMapping("/sub-recipe/{subRecipeId}")
    public ResponseEntity<List<PrepItemLocationDTO>> getBySubRecipe(@PathVariable Long subRecipeId) {
        List<PrepItemLocation> list = service.getBySubRecipe(subRecipeId);
        List<PrepItemLocationDTO> dtoList = list.stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(dtoList);
    }

    // GET by location
    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<PrepItemLocationDTO>> getByLocation(@PathVariable Long locationId) {
        List<PrepItemLocation> list = service.getByLocation(locationId);
        List<PrepItemLocationDTO> dtoList = list.stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(dtoList);
    }

    @PutMapping("/subrecipes/{subRecipeId}/locations/{locationId}/thresholds")
    public ResponseEntity<Void> setThresholdsForLocation(
            @PathVariable Long subRecipeId,
            @PathVariable Long locationId,
            @RequestBody ThresholdUpdateRequest request
    ) {
        service.setThresholdsForLocation(subRecipeId, locationId, request.minOnHand(), request.parLevel());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/subrecipes/{subRecipeId}/companies/{companyId}/thresholds")
    public ResponseEntity<Void> bulkSetThresholdsForCompany(
            @PathVariable Long subRecipeId,
            @PathVariable Long companyId,
            @RequestBody ThresholdUpdateRequest request
    ) {
        service.bulkSetThresholdsForCompany(companyId, subRecipeId, request.minOnHand(), request.parLevel());
        return ResponseEntity.ok().build();
    }

    public record ThresholdUpdateRequest(Double minOnHand, Double parLevel) {}

}
