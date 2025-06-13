package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.dto.InventoryItemListDTO;
import com.rayvision.inventory_management.model.dto.InventoryItemPartialUpdateDTO;
import com.rayvision.inventory_management.repository.CategoryRepository;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.service.InventoryItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryItemServiceImpl implements InventoryItemService {
    private final InventoryItemRepository inventoryItemRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;

    public InventoryItemServiceImpl(InventoryItemRepository inventoryItemRepository, CompanyRepository companyRepository, CategoryRepository categoryRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
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
    public InventoryItem partialUpdate(Long companyId, Long itemId, InventoryItemPartialUpdateDTO partialDto) {
        return inventoryItemRepository.findByCompanyIdAndId(companyId, itemId)
                .map(existingItem -> {
                    // For each field in partialDto, use Optional.ofNullable(...)
                    Optional.ofNullable(partialDto.getName()).ifPresent(existingItem::setName);
                    Optional.ofNullable(partialDto.getSku()).ifPresent(existingItem::setSku);
                    Optional.ofNullable(partialDto.getProductCode()).ifPresent(existingItem::setProductCode);
                    Optional.ofNullable(partialDto.getDescription()).ifPresent(existingItem::setDescription);
//                    Optional.ofNullable(partialDto.getCurrentPrice()).ifPresent(existingItem::setCurrentPrice);
                    Optional.ofNullable(partialDto.getCalories()).ifPresent(existingItem::setCalories);

                    // If you allow category or UOM changes, you'd do the same pattern or
                    // fetch them first if you need an ID. For example:
                    if (partialDto.getCategoryId() != null) {
                        Category cat = categoryRepository.findByCompanyIdAndId(companyId, partialDto.getCategoryId())
                            .orElseThrow(() -> new RuntimeException("Category not found"));
                        existingItem.setCategory(cat);
                    }

                    // Then save
                    return inventoryItemRepository.save(existingItem);
                })
                .orElseThrow(() -> new RuntimeException(
                        "Could not find inventory item with id " + itemId + " for company " + companyId
                ));
    }

    @Override
    public void deleteInventoryItemById(Long companyId, Long id) {
        inventoryItemRepository
                .findByCompanyIdAndId(companyId, id)
                .ifPresentOrElse(inventoryItemRepository::delete,
                        () -> { throw new RuntimeException("Could not find inventory item with id: " + id );}
                );
    }

    @Override
    public List<InventoryItem> searchItems(Long companyId, String searchTerm) {
        return inventoryItemRepository.searchInventoryItems(companyId, searchTerm);
    }

    @Override
    public Page<InventoryItem> getAllInventoryItemsPaginated(Long companyId, Pageable pageable) {
        return inventoryItemRepository.findByCompanyId(companyId, pageable);
    }

    @Override
    public Page<InventoryItem> searchItemsPaginated(Long companyId, String searchTerm, Pageable pageable) {
        return inventoryItemRepository.searchInventoryItems(companyId, searchTerm, pageable);
    }

    @Override
    public Page<InventoryItem> findByCompanyIdAndCategoryWithSearch(Long companyId, Long categoryId, String searchTerm, Pageable pageable) {
        return inventoryItemRepository.findByCompanyIdAndCategoryWithSearch(companyId, categoryId, searchTerm, pageable);
    }

    // Implement the method in your service implementation

    @Override
    public Page<InventoryItemListDTO> findInventoryItemsForListView(
            Long companyId,
            Long categoryId,
            String search,
            Pageable pageable) {

        return inventoryItemRepository.findInventoryItemsForListView(
                companyId,
                categoryId,
                search != null ? search : "",
                pageable);
    }
}
