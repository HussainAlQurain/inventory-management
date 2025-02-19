package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SubRecipeItem;

import java.util.List;
import java.util.Optional;

public interface SubRecipeItemService {
    /**
     * Get all items for a given SubRecipe.
     */
    List<SubRecipeItem> getItemsBySubRecipe(Long subRecipeId);

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
