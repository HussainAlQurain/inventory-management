package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.PrepItemLocation;

import java.util.List;
import java.util.Optional;

public interface PrepItemLocationService {
    /**
     * Get all PrepItemLocation rows for a given subRecipe or location.
     */
    List<PrepItemLocation> getBySubRecipe(Long subRecipeId);
    List<PrepItemLocation> getByLocation(Long locationId);

    /**
     * Create new bridging row linking subRecipe + location,
     * with minOnHand, par, etc.
     */
    PrepItemLocation create(Long subRecipeId, Long locationId, PrepItemLocation prepLoc);

    /**
     * Partial update of fields like onHand, minOnHand, par, etc.
     */
    PrepItemLocation update(Long bridgingId, PrepItemLocation patch);

    /**
     * Delete bridging row.
     */
    void delete(Long bridgingId);

    Optional<PrepItemLocation> getOne(Long bridgingId);
}
