package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.service.InventoryItemService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryItemServiceImpl implements InventoryItemService {
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryItemServiceImpl(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public List<InventoryItem> getAllInventoryItems() {
        return inventoryItemRepository.findAll();
    }

    @Override
    public Optional<InventoryItem> getInventoryItemById(Long id) {
        return inventoryItemRepository.findById(id);
    }

    @Override
    public InventoryItem save(InventoryItem inventoryItem) {
        return inventoryItemRepository.save(inventoryItem);
    }

    @Override
    public InventoryItem update(InventoryItem inventoryItem) {
        return null;
    }

    @Override
    public InventoryItem partialUpdate(InventoryItem inventoryItem) {
        return inventoryItemRepository.findById(inventoryItem.getId()).map(existingItem -> {
            Optional.ofNullable(inventoryItem.getName()).ifPresent(existingItem::setName);
            Optional.ofNullable(inventoryItem.getSku()).ifPresent(existingItem::setSku);
            Optional.ofNullable(inventoryItem.getProductCode()).ifPresent(existingItem::setProductCode);
            Optional.ofNullable(inventoryItem.getDescription()).ifPresent(existingItem::setDescription);
            Optional.ofNullable(inventoryItem.getCurrentPrice()).ifPresent(existingItem::setCurrentPrice);
            Optional.ofNullable(inventoryItem.getCalories()).ifPresent(existingItem::setCalories);
            Optional.ofNullable(inventoryItem.getCategory()).ifPresent(existingItem::setCategory);
            Optional.ofNullable(inventoryItem.getInventoryUom()).ifPresent(existingItem::setInventoryUom);
            return existingItem;
        }).orElseThrow(() -> new RuntimeException("Could not find inventory item with id: " + inventoryItem.getId()));
    }

    @Override
    public void deleteInventoryItemById(Long id) {
        inventoryItemRepository
                .findById(id)
                .ifPresentOrElse(existingItem -> inventoryItemRepository.delete(existingItem),
                        () -> { throw new RuntimeException("Could not find inventory item with id: " + id );}
                );
    }
}
