package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SubRecipe;

import java.util.List;
import java.util.Optional;

public interface SubRecipeService {
    /**
     * Retrieve all SubRecipes for a given company.
     */
    List<SubRecipe> getAllSubRecipes(Long companyId);

    /**
     * Retrieve a single SubRecipe by id for a given company.
     */
    Optional<SubRecipe> getSubRecipeById(Long companyId, Long subRecipeId);

    /**
     * Create a new SubRecipe, linking it to the specified company.
     */
    SubRecipe createSubRecipe(Long companyId, SubRecipe subRecipe);

    /**
     * Update an existing SubRecipe fully.
     */
    SubRecipe updateSubRecipe(Long companyId, SubRecipe subRecipe);

    /**
     * Partially update fields of a SubRecipe.
     */
    SubRecipe partialUpdateSubRecipe(Long companyId, SubRecipe subRecipe);

    /**
     * Delete a SubRecipe by id (for a given company).
     */
    void deleteSubRecipeById(Long companyId, Long subRecipeId);

}
