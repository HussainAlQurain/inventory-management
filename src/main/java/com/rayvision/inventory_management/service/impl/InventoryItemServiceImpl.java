package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.service.InventoryItemService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryItemServiceImpl implements InventoryItemService {
    private final InventoryItemRepository inventoryItemRepository;
    private final CompanyRepository companyRepository;

    public InventoryItemServiceImpl(InventoryItemRepository inventoryItemRepository, CompanyRepository companyRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<InventoryItem> getAllInventoryItems(Long companyId) {
        return inventoryItemRepository.findByCompanyId(companyId);
    }

    @Override
    public Optional<InventoryItem> getInventoryItemById(Long companyId, Long id) {
        return inventoryItemRepository.findByCompanyIdAndId(companyId, id);
    }

    @Override
    public InventoryItem save(Long companyId, InventoryItem inventoryItem) {
        return companyRepository.findById(companyId).map(company -> {
            inventoryItem.setCompany(company);
            return inventoryItemRepository.save(inventoryItem);
        }).orElseThrow(() -> new RuntimeException("Invalid companyId: " + companyId));
    }

    @Override
    public InventoryItem update(Long companyId, InventoryItem inventoryItem) {
        return null;
    }

    @Override
    public InventoryItem partialUpdate(Long companyId, InventoryItem inventoryItem) {
        return inventoryItemRepository.findByCompanyIdAndId(companyId, inventoryItem.getId()).map(existingItem -> {
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
    public void deleteInventoryItemById(Long companyId, Long id) {
        inventoryItemRepository
                .findByCompanyIdAndId(companyId, id)
                .ifPresentOrElse(existingItem -> inventoryItemRepository.delete(existingItem),
                        () -> { throw new RuntimeException("Could not find inventory item with id: " + id );}
                );
    }
}
