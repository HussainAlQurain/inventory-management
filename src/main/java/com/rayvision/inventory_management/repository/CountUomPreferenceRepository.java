package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.CountUomPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountUomPreferenceRepository extends JpaRepository<CountUomPreference, Long> {
    // If you plan to store preferences for an item or subRecipe:
    List<CountUomPreference> findByInventoryItemId(Long inventoryItemId);
    List<CountUomPreference> findBySubRecipeId(Long subRecipeId);

    // For removing a specific preference if you want “one preference per (item, subRecipe, uom)”
    Optional<CountUomPreference> findByInventoryItemIdAndCountUomId(Long itemId, Long uomId);
    Optional<CountUomPreference> findBySubRecipeIdAndCountUomId(Long subRecipeId, Long uomId);
}
