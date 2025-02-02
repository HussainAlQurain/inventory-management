package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryItem;

import java.util.List;
import java.util.Optional;

public interface InventoryItemService {
    List<InventoryItem> getAllInventoryItems();
    Optional<InventoryItem> getInventoryItemById(Long id);
    InventoryItem save(InventoryItem inventoryItem);
    InventoryItem update(InventoryItem inventoryItem);
    InventoryItem partialUpdate(InventoryItem inventoryItem);
    void deleteInventoryItemById(Long id);
}
