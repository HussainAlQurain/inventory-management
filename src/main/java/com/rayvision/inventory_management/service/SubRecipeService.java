package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.dto.SubRecipeCreateDTO;

import java.util.List;
import java.util.Optional;

public interface SubRecipeService {
    List<SubRecipe> getAllSubRecipes(Long companyId);

    Optional<SubRecipe> getSubRecipeById(Long companyId, Long subRecipeId);

    /**
     * Creates a new subRecipe plus bridging lines from the DTO
     */
    SubRecipe createSubRecipe(Long companyId, SubRecipeCreateDTO dto);

    /**
     * Update (full) a subRecipe, re-building bridging lines
     */
    SubRecipe updateSubRecipe(Long companyId, Long subRecipeId, SubRecipeCreateDTO dto);

    /**
     * Partial update if the user only wants to change certain fields or bridging lines
     */
    SubRecipe partialUpdateSubRecipe(Long companyId, Long subRecipeId, SubRecipeCreateDTO dto);

    void deleteSubRecipeById(Long companyId, Long subRecipeId);

    /**
     * Recompute cost from bridging lines (inventory items)
     */
    SubRecipe recalcSubRecipeCost(SubRecipe subRecipe);

    List<SubRecipe> searchSubRecipes(Long companyId, String searchTerm)
}
