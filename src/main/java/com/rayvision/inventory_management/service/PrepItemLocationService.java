package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.PrepItemLocation;
import com.rayvision.inventory_management.model.dto.PrepItemLocationDTO;

import java.util.List;
import java.util.Optional;

public interface PrepItemLocationService {
    // If you want to create from a DTO that includes subRecipeId + locationId
    PrepItemLocation create(PrepItemLocationDTO dto);

    // Partial or full update
    PrepItemLocation update(Long bridgingId, PrepItemLocationDTO dto);

    // If you want a "createOrUpdateBySubRecipeAndLocation" approach:
    PrepItemLocation createOrUpdate(PrepItemLocationDTO dto);

    void delete(Long bridgingId);
    Optional<PrepItemLocation> getOne(Long bridgingId);

    // Existing queries
    List<PrepItemLocation> getBySubRecipe(Long subRecipeId);
    List<PrepItemLocation> getByLocation(Long locationId);

}
