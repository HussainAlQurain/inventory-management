package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.dto.InventoryItemPartialUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InventoryItemService {
    List<InventoryItem> getAllInventoryItems(Long companyId);
    Optional<InventoryItem> getInventoryItemById(Long companyId ,Long id);
    InventoryItem save(Long companyId, InventoryItem inventoryItem);
    InventoryItem update(Long companyId, InventoryItem inventoryItem);
    InventoryItem partialUpdate(Long companyId, Long itemId, InventoryItemPartialUpdateDTO inventoryItemPartialUpdateDTO);
    void deleteInventoryItemById(Long companyId, Long id);
    List<InventoryItem> searchItems(Long companyId, String searchTerm);
    
    Page<InventoryItem> getAllInventoryItemsPaginated(Long companyId, Pageable pageable);
    Page<InventoryItem> searchItemsPaginated(Long companyId, String searchTerm, Pageable pageable);
    Page<InventoryItem> findByCompanyIdAndCategoryWithSearch(Long companyId, Long categoryId, String searchTerm, Pageable pageable);
}
