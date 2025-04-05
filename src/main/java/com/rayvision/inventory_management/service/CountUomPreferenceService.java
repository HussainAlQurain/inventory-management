package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.CountUomPreference;
import com.rayvision.inventory_management.model.UnitOfMeasure;

import java.util.List;

public interface CountUomPreferenceService {

    // 1) For InventoryItem
    CountUomPreference addPreferenceForItem(Long itemId, Long uomId, boolean defaultUom);
    void removePreferenceForItem(Long itemId, Long uomId);
    List<CountUomPreference> getPreferencesForItem(Long itemId);

    // 2) For SubRecipe
    CountUomPreference addPreferenceForSubRecipe(Long subRecipeId, Long uomId, boolean defaultUom);
    void removePreferenceForSubRecipe(Long subRecipeId, Long uomId);
    List<CountUomPreference> getPreferencesForSubRecipe(Long subRecipeId);

    // 3) For the UI: get available UOM by same category
    // e.g. "WEIGHT" for an item that has inventoryUom's category "WEIGHT"
    List<UnitOfMeasure> getAvailableUomsForItem(Long itemId);
    List<UnitOfMeasure> getAvailableUomsForSubRecipe(Long subRecipeId);

}
