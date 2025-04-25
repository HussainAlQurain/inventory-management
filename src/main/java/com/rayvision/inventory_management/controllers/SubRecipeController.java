package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.SubRecipeMapper;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.dto.SubRecipeCreateDTO;
import com.rayvision.inventory_management.model.dto.SubRecipeDTO;
import com.rayvision.inventory_management.service.SubRecipeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sub-recipes")
public class SubRecipeController {

    private final SubRecipeService subRecipeService;
    private final SubRecipeMapper subRecipeMapper;

    public SubRecipeController(SubRecipeService subRecipeService, SubRecipeMapper subRecipeMapper) {
        this.subRecipeService = subRecipeService;
        this.subRecipeMapper = subRecipeMapper;
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<Map<String, Object>> getAllSubRecipes(
            @PathVariable Long companyId,
            @RequestParam(name = "search", required = false, defaultValue = "") String searchTerm,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "name") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
            
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<SubRecipe> pageResults = subRecipeService.searchSubRecipes(companyId, searchTerm, pageable);
        
        List<SubRecipeDTO> dtoList = pageResults.getContent().stream()
                .map(subRecipeMapper::toDto)
                .toList();
                
        Map<String, Object> response = new HashMap<>();
        response.put("items", dtoList);
        response.put("currentPage", pageResults.getNumber());
        response.put("totalItems", pageResults.getTotalElements());
        response.put("totalPages", pageResults.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipeDTO> getSubRecipeById(@PathVariable Long subRecipeId,
                                                         @PathVariable Long companyId) {
        return subRecipeService.getSubRecipeById(companyId, subRecipeId)
                .map(subRecipeMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/company/{companyId}")
    public ResponseEntity<SubRecipeDTO> createSubRecipe(@PathVariable Long companyId,
                                                     @RequestBody SubRecipeCreateDTO dto) {
        SubRecipe created = subRecipeService.createSubRecipe(companyId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(subRecipeMapper.toDto(created));
    }

    @PutMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipeDTO> updateSubRecipe(
            @PathVariable Long companyId,
            @PathVariable Long subRecipeId,
            @Valid @RequestBody SubRecipeCreateDTO dto) {

        SubRecipe updated = subRecipeService.updateSubRecipe(companyId, subRecipeId, dto);
        return ResponseEntity.ok(subRecipeMapper.toDto(updated));
    }

    @PatchMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipeDTO> partialUpdateSubRecipe(@PathVariable Long companyId,
                                                            @PathVariable Long subRecipeId,
                                                            @RequestBody SubRecipeCreateDTO dto) {
        try {
            SubRecipe updated = subRecipeService.partialUpdateSubRecipe(companyId, subRecipeId, dto);
            return ResponseEntity.ok(subRecipeMapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<Void> deleteSubRecipe(@PathVariable Long companyId,
                                                @PathVariable Long subRecipeId) {
        try {
            subRecipeService.deleteSubRecipeById(companyId, subRecipeId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
