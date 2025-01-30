package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryItem;

import java.util.List;

public interface InventoryItemService {
    List<InventoryItem> getAllInventoryItems();
    InventoryItem getInventoryItemById(int id);
    InventoryItem save(InventoryItem inventoryItem);
    InventoryItem update(InventoryItem inventoryItem);
    InventoryItem partialUpdate(InventoryItem inventoryItem);
    void deleteInventoryItemById(int id);
}
