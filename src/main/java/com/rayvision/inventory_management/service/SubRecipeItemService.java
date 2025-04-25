package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SubRecipeItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SubRecipeItemService {
    /**
     * Get all items for a given SubRecipe.
     */
    List<SubRecipeItem> getItemsBySubRecipe(Long subRecipeId);

    /**
     * Get items for a given SubRecipe with pagination support.
     * @param subRecipeId The ID of the sub-recipe
     * @param searchTerm Optional search term to filter results
     * @param pageable Pagination parameters
     * @return Page of sub-recipe items
     */
    Page<SubRecipeItem> getItemsBySubRecipe(Long subRecipeId, String searchTerm, Pageable pageable);

    /**
     * Create a new SubRecipeItem linked to the subRecipe.
     */
    SubRecipeItem createItem(Long subRecipeId, SubRecipeItem item);

    /**
     * Update or partial update a SubRecipeItem.
     */
    SubRecipeItem updateItem(Long subRecipeId, SubRecipeItem item);
    SubRecipeItem partialUpdateItem(Long subRecipeId, SubRecipeItem item);

    /**
     * Delete an item by ID, verifying it belongs to that subRecipe.
     */
    void deleteItem(Long subRecipeId, Long itemId);

    /**
     * Optional: fetch single item by subRecipe + itemId
     */
    Optional<SubRecipeItem> getOne(Long subRecipeId, Long itemId);

}
