package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryItem;

import java.util.List;
import java.util.Optional;

public interface InventoryItemService {
    List<InventoryItem> getAllInventoryItems(Long companyId);
    Optional<InventoryItem> getInventoryItemById(Long companyId ,Long id);
    InventoryItem save(Long companyId, InventoryItem inventoryItem);
    InventoryItem update(Long companyId, InventoryItem inventoryItem);
    InventoryItem partialUpdate(Long companyId, InventoryItem inventoryItem);
    void deleteInventoryItemById(Long companyId, Long id);
}
