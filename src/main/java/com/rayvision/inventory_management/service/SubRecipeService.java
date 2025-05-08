package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.dto.SubRecipeCreateDTO;
import com.rayvision.inventory_management.model.dto.SubRecipeListDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Search sub-recipes without pagination
     * @param companyId The company ID
     * @param searchTerm Search term to filter results
     * @return List of sub-recipes
     */
    List<SubRecipe> searchSubRecipes(Long companyId, String searchTerm);
    
    /**
     * Search sub-recipes with pagination support
     * @param companyId The company ID
     * @param searchTerm Search term to filter results
     * @param pageable Pagination parameters
     * @return Page of sub-recipes
     */
    Page<SubRecipe> searchSubRecipes(Long companyId, String searchTerm, Pageable pageable);

    Page<SubRecipeListDTO> searchSubRecipesLight(Long companyId, String searchTerm, Pageable pageable);
}
