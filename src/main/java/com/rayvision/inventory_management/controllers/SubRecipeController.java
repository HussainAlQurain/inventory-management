package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.dto.SubRecipeCreateDTO;
import com.rayvision.inventory_management.service.SubRecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sub-recipes")
public class SubRecipeController {

    private final SubRecipeService subRecipeService;

    public SubRecipeController(SubRecipeService subRecipeService) {
        this.subRecipeService = subRecipeService;
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<SubRecipe>> getAllSubRecipes(@PathVariable Long companyId) {
        List<SubRecipe> list = subRecipeService.getAllSubRecipes(companyId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipe> getSubRecipeById(@PathVariable Long companyId,
                                                      @PathVariable Long subRecipeId) {
        return subRecipeService.getSubRecipeById(companyId, subRecipeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CREATE using the new DTO
    @PostMapping("/company/{companyId}")
    public ResponseEntity<SubRecipe> createSubRecipe(@PathVariable Long companyId,
                                                     @RequestBody SubRecipeCreateDTO dto) {
        SubRecipe created = subRecipeService.createSubRecipe(companyId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // UPDATE (full replace) using the new DTO
    @PutMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipe> updateSubRecipe(@PathVariable Long companyId,
                                                     @PathVariable Long subRecipeId,
                                                     @RequestBody SubRecipeCreateDTO dto) {
        try {
            SubRecipe updated = subRecipeService.updateSubRecipe(companyId, subRecipeId, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // PARTIAL update
    @PatchMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipe> partialUpdateSubRecipe(@PathVariable Long companyId,
                                                            @PathVariable Long subRecipeId,
                                                            @RequestBody SubRecipeCreateDTO dto) {
        try {
            SubRecipe updated = subRecipeService.partialUpdateSubRecipe(companyId, subRecipeId, dto);
            return ResponseEntity.ok(updated);
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
