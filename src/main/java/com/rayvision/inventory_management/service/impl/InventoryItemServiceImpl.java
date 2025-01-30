package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.service.InventoryItemService;

import java.util.List;

public class InventoryItemServiceImpl implements InventoryItemService {
    private InventoryItemRepository inventoryItemRepository;

    public InventoryItemServiceImpl(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public List<InventoryItem> getAllInventoryItems() {
        return List.of();
    }

    @Override
    public InventoryItem getInventoryItemById(int id) {
        return null;
    }

    @Override
    public InventoryItem save(InventoryItem inventoryItem) {
        return null;
    }

    @Override
    public InventoryItem update(InventoryItem inventoryItem) {
        return null;
    }

    @Override
    public InventoryItem partialUpdate(InventoryItem inventoryItem) {
        return null;
    }

    @Override
    public void deleteInventoryItemById(int id) {

    }
}
