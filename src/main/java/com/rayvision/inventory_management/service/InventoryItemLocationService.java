package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryItemLocation;
import com.rayvision.inventory_management.model.dto.InventoryItemLocationDTO;

import java.util.List;
import java.util.Optional;

public interface InventoryItemLocationService {
    InventoryItemLocation create(InventoryItemLocationDTO dto);

    InventoryItemLocation update(Long bridgingId, InventoryItemLocationDTO dto);

    void delete(Long bridgingId);

    Optional<InventoryItemLocation> getOne(Long bridgingId);

    // If you want a convenient method to create or update by item+location
    InventoryItemLocation createOrUpdateByItemAndLocation(InventoryItemLocationDTO dto);

    List<InventoryItemLocation> getByItem(Long itemId);
    List<InventoryItemLocation> getByLocation(Long locationId);
    void bulkUpdate(Long companyId, Long itemId, Double newMin, Double newPar);
    List<InventoryItemLocation> findByItemId(Long itemId);

    void setThresholdsForLocation(Long itemId, Long locationId, Double minOnHand, Double parLevel);
    void bulkSetThresholdsForCompany(Long companyId, Long itemId, Double minOnHand, Double parLevel);
    void patchThresholds(Long itemId, Long locationId, Double minOnHand, Double parLevel);

    Optional<InventoryItemLocation> findByInventoryItemIdAndLocationId(Long itemId, Long locationId);
}
