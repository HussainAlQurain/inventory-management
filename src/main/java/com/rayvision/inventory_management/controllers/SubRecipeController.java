package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.SubRecipe;
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

    // GET all subRecipes for a company
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<SubRecipe>> getAllSubRecipes(@PathVariable Long companyId) {
        List<SubRecipe> list = subRecipeService.getAllSubRecipes(companyId);
        return ResponseEntity.ok(list);
    }

    // GET one subRecipe by id
    @GetMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipe> getSubRecipeById(@PathVariable Long companyId,
                                                      @PathVariable Long subRecipeId) {
        return subRecipeService.getSubRecipeById(companyId, subRecipeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CREATE new SubRecipe
    @PostMapping("/company/{companyId}")
    public ResponseEntity<SubRecipe> createSubRecipe(@PathVariable Long companyId,
                                                     @RequestBody SubRecipe subRecipe) {
        SubRecipe created = subRecipeService.createSubRecipe(companyId, subRecipe);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // UPDATE (full) SubRecipe
    @PutMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipe> updateSubRecipe(@PathVariable Long companyId,
                                                     @PathVariable Long subRecipeId,
                                                     @RequestBody SubRecipe subRecipe) {
        // Ensure subRecipe entity’s id matches the path
        subRecipe.setId(subRecipeId);
        try {
            SubRecipe updated = subRecipeService.updateSubRecipe(companyId, subRecipe);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // PARTIAL update
    @PatchMapping("/{subRecipeId}/company/{companyId}")
    public ResponseEntity<SubRecipe> partialUpdateSubRecipe(@PathVariable Long companyId,
                                                            @PathVariable Long subRecipeId,
                                                            @RequestBody SubRecipe patchEntity) {
        patchEntity.setId(subRecipeId);
        try {
            SubRecipe updated = subRecipeService.partialUpdateSubRecipe(companyId, patchEntity);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE SubRecipe
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
