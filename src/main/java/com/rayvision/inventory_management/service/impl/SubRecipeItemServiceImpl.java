package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.SubRecipeItem;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.repository.SubRecipeItemRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.repository.UnitOfMeasureRepository;
import com.rayvision.inventory_management.service.SubRecipeItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubRecipeItemServiceImpl implements SubRecipeItemService {
    private final SubRecipeItemRepository subRecipeItemRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;

    public SubRecipeItemServiceImpl(SubRecipeItemRepository subRecipeItemRepository,
                                    SubRecipeRepository subRecipeRepository,
                                    UnitOfMeasureRepository unitOfMeasureRepository) {
        this.subRecipeItemRepository = subRecipeItemRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
    }

    @Override
    public List<SubRecipeItem> getItemsBySubRecipe(Long subRecipeId) {
        List<SubRecipeItem> all = subRecipeItemRepository.findAll();
        return all.stream()
                .filter(item -> item.getSubRecipe().getId().equals(subRecipeId))
                .toList();
    }

    @Override
    public Page<SubRecipeItem> getItemsBySubRecipe(Long subRecipeId, String searchTerm, Pageable pageable) {
        List<SubRecipeItem> all = subRecipeItemRepository.findAll();

        // Filter by subRecipeId and optional search term
        List<SubRecipeItem> filtered = all.stream()
                .filter(item -> item.getSubRecipe().getId().equals(subRecipeId))
                .filter(item -> {
                    // Apply search term filter if provided
                    if (!StringUtils.hasText(searchTerm)) {
                        return true;
                    }

                    String searchLower = searchTerm.toLowerCase();

                    // Search in inventory item name if present
                    if (item.getInventoryItem() != null &&
                        item.getInventoryItem().getName() != null &&
                        item.getInventoryItem().getName().toLowerCase().contains(searchLower)) {
                        return true;
                    }

                    // Add any other properties you want to include in the search

                    return false;
                })
                .collect(Collectors.toList());

        // Manual pagination - not efficient for large datasets
        // In a real-world scenario, you'd want to implement repository methods that handle pagination at the DB level
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());

        if (start > filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }

        List<SubRecipeItem> pageContent = filtered.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    public Optional<SubRecipeItem> getOne(Long subRecipeId, Long itemId) {
        return subRecipeItemRepository.findById(itemId)
                .filter(item -> item.getSubRecipe().getId().equals(subRecipeId));
    }

    @Override
    public SubRecipeItem createItem(Long subRecipeId, SubRecipeItem item) {
        SubRecipe parent = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + subRecipeId));

        item.setSubRecipe(parent);

        if (item.getUnitOfMeasure() != null && item.getUnitOfMeasure().getId() != null) {
            UnitOfMeasure existingUom = unitOfMeasureRepository.findById(item.getUnitOfMeasure().getId())
                    .orElseThrow(() -> new RuntimeException("UOM not found for: " + item.getUnitOfMeasure().getId()));
            item.setUnitOfMeasure(existingUom);
        }

        return subRecipeItemRepository.save(item);
    }

    @Override
    public SubRecipeItem updateItem(Long subRecipeId, SubRecipeItem item) {
        SubRecipeItem existing = subRecipeItemRepository.findById(item.getId())
                .filter(i -> i.getSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException("Item not found or not in subRecipe " + subRecipeId));

        existing.setQuantity(item.getQuantity());
        existing.setWastagePercent(item.getWastagePercent());
        if (item.getUnitOfMeasure() != null) {
            existing.setUnitOfMeasure(item.getUnitOfMeasure());
        }
        if (item.getInventoryItem() != null) {
            existing.setInventoryItem(item.getInventoryItem());
        }
        return subRecipeItemRepository.save(existing);
    }

    @Override
    public SubRecipeItem partialUpdateItem(Long subRecipeId, SubRecipeItem item) {
        SubRecipeItem existing = subRecipeItemRepository.findById(item.getId())
                .filter(i -> i.getSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException("Item not found or not in subRecipe " + subRecipeId));

        if (item.getQuantity() != null) existing.setQuantity(item.getQuantity());
        if (item.getWastagePercent() != null) existing.setWastagePercent(item.getWastagePercent());
        if (item.getUnitOfMeasure() != null) {
            existing.setUnitOfMeasure(item.getUnitOfMeasure());
        }
        if (item.getInventoryItem() != null) {
            existing.setInventoryItem(item.getInventoryItem());
        }
        return subRecipeItemRepository.save(existing);
    }

    @Override
    public void deleteItem(Long subRecipeId, Long itemId) {
        SubRecipeItem existing = subRecipeItemRepository.findById(itemId)
                .filter(i -> i.getSubRecipe().getId().equals(subRecipeId))
                .orElseThrow(() -> new RuntimeException("Item not found or not in subRecipe " + subRecipeId));
        subRecipeItemRepository.delete(existing);
    }

}
